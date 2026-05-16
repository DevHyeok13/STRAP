#include <jni.h>
#include <vector>
#include <cmath>
#include <random>
#include <algorithm>
#include <array>
#include <Eigen/Dense>
#include <Eigen/Geometry>

// ★ 13개의 각도 인덱스 정의
enum GeneIndex {
    SPINE_PITCH = 0,
    L_SH_FLEX, L_SH_ABD, L_EL_FLEX,
    R_SH_FLEX, R_SH_ABD, R_EL_FLEX,
    L_HIP_FLEX, L_HIP_ABD, L_KNEE_FLEX,
    R_HIP_FLEX, R_HIP_ABD, R_KNEE_FLEX,
    NUM_GENES
};

// 🚀 [완전 진화] True 3D + 계층적 역운동학(Hierarchical IK) 손실 함수
float calculateLoss3D(const std::vector<Eigen::Vector3f>& target3D, const std::array<float, NUM_GENES>& genes) {
    const float SPINE_LEN = 400.0f; const float CLAVICLE_LEN = 150.0f; const float PELVIS_WIDTH = 120.0f;
    const float UPPER_ARM = 250.0f; const float LOWER_ARM = 200.0f;
    const float THIGH_LEN = 350.0f; const float CALF_LEN = 350.0f;

    Eigen::Vector3f pelvis(0, 0, 0); // 3D 공간의 영점 고정

    // 1. 척추 회전
    Eigen::Quaternionf r_spine = Eigen::AngleAxisf(genes[SPINE_PITCH], Eigen::Vector3f::UnitX()) * Eigen::Quaternionf::Identity();
    Eigen::Vector3f neck = pelvis + r_spine * Eigen::Vector3f(0, -SPINE_LEN, 0);

    // 2. 왼쪽 팔 (어깨 -> 팔꿈치 계층적 누적 + Z축 Abd)
    Eigen::Vector3f l_sh_base = neck + r_spine * Eigen::Vector3f(-CLAVICLE_LEN, 0, 0);
    Eigen::Quaternionf r_l_sh = r_spine * Eigen::AngleAxisf(genes[L_SH_FLEX], Eigen::Vector3f::UnitX()) * Eigen::AngleAxisf(genes[L_SH_ABD], Eigen::Vector3f::UnitZ());
    Eigen::Vector3f l_elbow = l_sh_base + r_l_sh * Eigen::Vector3f(0, UPPER_ARM, 0);
    Eigen::Quaternionf r_l_el = r_l_sh * Eigen::AngleAxisf(genes[L_EL_FLEX], Eigen::Vector3f::UnitX());
    Eigen::Vector3f l_wrist = l_elbow + r_l_el * Eigen::Vector3f(0, LOWER_ARM, 0);

    // 3. 오른쪽 팔
    Eigen::Vector3f r_sh_base = neck + r_spine * Eigen::Vector3f(CLAVICLE_LEN, 0, 0);
    Eigen::Quaternionf r_r_sh = r_spine * Eigen::AngleAxisf(genes[R_SH_FLEX], Eigen::Vector3f::UnitX()) * Eigen::AngleAxisf(genes[R_SH_ABD], Eigen::Vector3f::UnitZ());
    Eigen::Vector3f r_elbow = r_sh_base + r_r_sh * Eigen::Vector3f(0, UPPER_ARM, 0);
    Eigen::Quaternionf r_r_el = r_r_sh * Eigen::AngleAxisf(genes[R_EL_FLEX], Eigen::Vector3f::UnitX());
    Eigen::Vector3f r_wrist = r_elbow + r_r_el * Eigen::Vector3f(0, LOWER_ARM, 0);

    // 4. 왼쪽 다리
    Eigen::Vector3f l_hip_base = pelvis + Eigen::Vector3f(-PELVIS_WIDTH, 0, 0);
    Eigen::Quaternionf r_l_hip = Eigen::AngleAxisf(genes[L_HIP_FLEX], Eigen::Vector3f::UnitX()) * Eigen::AngleAxisf(genes[L_HIP_ABD], Eigen::Vector3f::UnitZ());
    Eigen::Vector3f l_knee = l_hip_base + r_l_hip * Eigen::Vector3f(0, THIGH_LEN, 0);
    Eigen::Quaternionf r_l_knee = r_l_hip * Eigen::AngleAxisf(genes[L_KNEE_FLEX], Eigen::Vector3f::UnitX());
    Eigen::Vector3f l_ankle = l_knee + r_l_knee * Eigen::Vector3f(0, CALF_LEN, 0);

    // 5. 오른쪽 다리
    Eigen::Vector3f r_hip_base = pelvis + Eigen::Vector3f(PELVIS_WIDTH, 0, 0);
    Eigen::Quaternionf r_r_hip = Eigen::AngleAxisf(genes[R_HIP_FLEX], Eigen::Vector3f::UnitX()) * Eigen::AngleAxisf(genes[R_HIP_ABD], Eigen::Vector3f::UnitZ());
    Eigen::Vector3f r_knee = r_hip_base + r_r_hip * Eigen::Vector3f(0, THIGH_LEN, 0);
    Eigen::Quaternionf r_r_knee = r_r_hip * Eigen::AngleAxisf(genes[R_KNEE_FLEX], Eigen::Vector3f::UnitX());
    Eigen::Vector3f r_ankle = r_knee + r_r_knee * Eigen::Vector3f(0, CALF_LEN, 0);

    // 6. 3D 유클리드 오차 계산 (2D 투영 제거)
    float error = 0.0f;
    std::vector<Eigen::Vector3f> joints3d = {l_sh_base, r_sh_base, l_elbow, r_elbow, l_wrist, r_wrist, l_hip_base, r_hip_base, l_knee, r_knee, l_ankle, r_ankle};
    for (int i = 0; i < 12; ++i) {
        error += (joints3d[i] - target3D[i]).norm();
    }

// ---------------------------------------------------------------------
    // 7. 인체공학적 관절 가동 범위 (ROM) 패널티 적용 (Soft Constraints)
    // ---------------------------------------------------------------------

    // 💡 패널티 계산용 람다 함수 (범위를 벗어난 각도의 '제곱'만큼 벌점 부여)
    auto applyPenalty = [](float value_rad, float min_deg, float max_deg) {
        float min_rad = min_deg * M_PI / 180.0f;
        float max_rad = max_deg * M_PI / 180.0f;

        if (value_rad < min_rad) {
            return std::pow(min_rad - value_rad, 2.0f) * 5000.0f; // 제곱 패널티
        }
        if (value_rad > max_rad) {
            return std::pow(value_rad - max_rad, 2.0f) * 5000.0f; // 제곱 패널티
        }
        return 0.0f; // 정상 범위면 무죄(0점)
    };

    // 🦴 척추: 뒤로 가볍게 젖히기(-30도) ~ 앞으로 폴더처럼 숙이기(120도)
    error += applyPenalty(genes[SPINE_PITCH], -30.0f, 120.0f);

    // 🦴 어깨: 뒤로 뻗기(-50도) ~ 위로 만세(180도) / 몸통 교차(-45도) ~ 옆으로 만세(180도)
    error += applyPenalty(genes[L_SH_FLEX], -50.0f, 180.0f);
    error += applyPenalty(genes[L_SH_ABD],  -45.0f, 180.0f);
    error += applyPenalty(genes[R_SH_FLEX], -50.0f, 180.0f);
    error += applyPenalty(genes[R_SH_ABD],  -45.0f, 180.0f);

    // 🦴 팔꿈치: 약간의 과신전(-10도) ~ 이두근에 닿는 최대 굽힘(150도)
    error += applyPenalty(genes[L_EL_FLEX], -10.0f, 150.0f);
    error += applyPenalty(genes[R_EL_FLEX], -10.0f, 150.0f);

    // 🦴 고관절: 다리 뒤로 뻗기(-30도) ~ 무릎 가슴에 닿기(120도) / 다리 모으기(-30도) ~ 쩍벌(90도)
    error += applyPenalty(genes[L_HIP_FLEX], -30.0f, 120.0f);
    error += applyPenalty(genes[L_HIP_ABD],  -30.0f,  90.0f);
    error += applyPenalty(genes[R_HIP_FLEX], -30.0f, 120.0f);
    error += applyPenalty(genes[R_HIP_ABD],  -30.0f,  90.0f);

    // 🦴 무릎: 약간의 과신전(-10도) ~ 허벅지와 종아리가 닿는 최대 굽힘(150도)
    error += applyPenalty(genes[L_KNEE_FLEX], -10.0f, 150.0f);
    error += applyPenalty(genes[R_KNEE_FLEX], -10.0f, 150.0f);

    return error;
}

// 🚀 누락되었던 구조체 추가!
struct Individual {
    std::array<float, NUM_GENES> genes;
    float fitness;
};

// JNI 통신 함수
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_strapxml_HumanoidOptimizer_runOptimization(
        JNIEnv* env, jobject /* this */, jfloatArray worldLandmarks3D) {

    jfloat* data = env->GetFloatArrayElements(worldLandmarks3D, nullptr);

    // Kotlin에서 넘어온 36개의 3D 좌표(X,Y,Z) 수신
    std::vector<Eigen::Vector3f> target3D;
    for (int i = 0; i < 12; ++i) {
        target3D.emplace_back(data[i * 3], data[i * 3 + 1], data[i * 3 + 2]);
    }
    env->ReleaseFloatArrayElements(worldLandmarks3D, data, 0);

    const int POPULATION_SIZE = 150;
    const int MAX_GENERATION = 50;
    const float SELECTION_RATIO = 0.2f;
    const float SHRINK_FACTOR = 0.85f;

    // 💡 [핵심 1] 최소 탐색 범위 보장. 표준편차가 이 값보다 낮아지면 강제로 끌어올립니다.
    const float MIN_STD = 0.0436f; // 약 2.5도의 최소 탐색 각도

    int eliteCount = static_cast<int>(static_cast<float>(POPULATION_SIZE) * SELECTION_RATIO);
    std::array<float, NUM_GENES> means = {0.0f};
    std::array<float, NUM_GENES> stds;
    std::fill(stds.begin(), stds.end(), static_cast<float>(M_PI) / 2.0f);

    // 💡 [핵심 2] 시드 42 고정 해제. 매 프레임 완전히 새로운 난수로 Local Minima 함정 회피!
    std::random_device rd;
    std::mt19937 gen(rd());

    // 💡 [핵심 3] 돌연변이 생성을 위한 완전 무작위 분포 (-180도 ~ 180도)
    std::uniform_real_distribution<float> mutDist(static_cast<float>(-M_PI), static_cast<float>(M_PI));

    Individual bestOverall; bestOverall.fitness = 999999.0f;

    for (int genIdx = 0; genIdx < MAX_GENERATION; ++genIdx) {
        std::vector<Individual> population(POPULATION_SIZE);
        std::vector<std::normal_distribution<float>> dists;
        for (int j = 0; j < NUM_GENES; ++j) dists.emplace_back(means[j], stds[j]);

        for (int i = 0; i < POPULATION_SIZE; ++i) {
            // 💡 [핵심 4] 인구의 하위 10%에게 돌연변이(강제 무작위 각도) 주입!
            if (i >= POPULATION_SIZE * 0.9f) {
                for (int j = 0; j < NUM_GENES; ++j) {
                    population[i].genes[j] = mutDist(gen);
                }
            } else {
                for (int j = 0; j < NUM_GENES; ++j) {
                    population[i].genes[j] = dists[j](gen);
                }
            }
            population[i].fitness = calculateLoss3D(target3D, population[i].genes);
        }

        std::sort(population.begin(), population.end(), [](const Individual& a, const Individual& b) { return a.fitness < b.fitness; });
        if (population[0].fitness < bestOverall.fitness) bestOverall = population[0];

        std::array<float, NUM_GENES> new_means = {0.0f};
        for (int i = 0; i < eliteCount; ++i) {
            for (int j = 0; j < NUM_GENES; ++j) new_means[j] += population[i].genes[j];
        }
        for (int j = 0; j < NUM_GENES; ++j) means[j] = new_means[j] / static_cast<float>(eliteCount);

        std::array<float, NUM_GENES> new_vars = {0.0f};
        for (int i = 0; i < eliteCount; ++i) {
            for (int j = 0; j < NUM_GENES; ++j) {
                float diff = population[i].genes[j] - means[j];
                new_vars[j] += diff * diff;
            }
        }
        for (int j = 0; j < NUM_GENES; ++j) {
            stds[j] = std::sqrt(new_vars[j] / static_cast<float>(eliteCount)) * SHRINK_FACTOR;

            // 💡 [핵심 5] 안전장치 가동! 표준편차가 0으로 수렴하려 하면 최소값으로 방어!
            if (stds[j] < MIN_STD) {
                stds[j] = MIN_STD;
            }
        }
    }

    std::vector<float> resultAngles(NUM_GENES);
    for(int i = 0; i < NUM_GENES; ++i) resultAngles[i] = bestOverall.genes[i] * 180.0f / static_cast<float>(M_PI);

    jfloatArray resultArray = env->NewFloatArray(static_cast<jsize>(resultAngles.size()));
    env->SetFloatArrayRegion(resultArray, 0, static_cast<jsize>(resultAngles.size()), resultAngles.data());

    return resultArray;
}