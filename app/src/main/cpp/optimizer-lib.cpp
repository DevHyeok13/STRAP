#include <jni.h>
#include <vector>
#include <cmath>
#include <random>
#include <algorithm>
#include <array>
#include <Eigen/Dense>
#include <Eigen/Geometry>

// 1. 카메라 투영 행렬
Eigen::Matrix3f getCameraIntrinsics() {
    Eigen::Matrix3f K = Eigen::Matrix3f::Identity();
    K(0, 0) = 1000.0f; K(1, 1) = 1000.0f;
    K(0, 2) = 540.0f; K(1, 2) = 960.0f;
    return K;
}

// 2. 3D 관절 회전 및 위치 계산
Eigen::Vector3f calculateJointPosition(const Eigen::Vector3f& parent, const Eigen::Vector3f& link, float aX, float aY, float aZ) {
    Eigen::Quaternionf q = Eigen::AngleAxisf(aX, Eigen::Vector3f::UnitX()) * Eigen::AngleAxisf(aY, Eigen::Vector3f::UnitY()) * Eigen::AngleAxisf(aZ, Eigen::Vector3f::UnitZ());
    return parent + q.matrix() * link;
}

// ★ 13개의 각도 인덱스 정의 (전신)
enum GeneIndex {
    SPINE_PITCH = 0,
    L_SH_FLEX, L_SH_ABD, L_EL_FLEX, // 좌측 팔
    R_SH_FLEX, R_SH_ABD, R_EL_FLEX, // 우측 팔
    L_HIP_FLEX, L_HIP_ABD, L_KNEE_FLEX, // 좌측 다리
    R_HIP_FLEX, R_HIP_ABD, R_KNEE_FLEX, // 우측 다리
    NUM_GENES // 총 13개
};

// 3. 확장된 전신 손실 함수
float calculateLoss(const std::vector<Eigen::Vector2f>& target2D, const std::array<float, NUM_GENES>& genes) {
    // [가상의 인체 뼈대 비율]
    const float SPINE_LEN = 400.0f;
    const float CLAVICLE_LEN = 150.0f;
    const float PELVIS_WIDTH = 120.0f;
    const float UPPER_ARM = 250.0f; const float LOWER_ARM = 200.0f;
    const float THIGH_LEN = 350.0f; const float CALF_LEN = 350.0f;

    Eigen::Vector3f pelvis(0, 0, 2500.0f); // 기준점 (Root)

    // [상체 Kinematics]
    Eigen::Vector3f neck = calculateJointPosition(pelvis, Eigen::Vector3f(0, -SPINE_LEN, 0), genes[SPINE_PITCH], 0, 0);

    Eigen::Vector3f l_sh_base = neck + Eigen::Vector3f(-CLAVICLE_LEN, 0, 0);
    Eigen::Vector3f l_elbow = calculateJointPosition(l_sh_base, Eigen::Vector3f(0, UPPER_ARM, 0), genes[L_SH_FLEX], genes[L_SH_ABD], 0);
    Eigen::Vector3f l_wrist = calculateJointPosition(l_elbow, Eigen::Vector3f(0, LOWER_ARM, 0), genes[L_EL_FLEX], 0, 0);

    Eigen::Vector3f r_sh_base = neck + Eigen::Vector3f(CLAVICLE_LEN, 0, 0);
    Eigen::Vector3f r_elbow = calculateJointPosition(r_sh_base, Eigen::Vector3f(0, UPPER_ARM, 0), genes[R_SH_FLEX], genes[R_SH_ABD], 0);
    Eigen::Vector3f r_wrist = calculateJointPosition(r_elbow, Eigen::Vector3f(0, LOWER_ARM, 0), genes[R_EL_FLEX], 0, 0);

    // [하체 Kinematics]
    Eigen::Vector3f l_hip_base = pelvis + Eigen::Vector3f(-PELVIS_WIDTH, 0, 0);
    Eigen::Vector3f l_knee = calculateJointPosition(l_hip_base, Eigen::Vector3f(0, THIGH_LEN, 0), genes[L_HIP_FLEX], genes[L_HIP_ABD], 0);
    Eigen::Vector3f l_ankle = calculateJointPosition(l_knee, Eigen::Vector3f(0, CALF_LEN, 0), genes[L_KNEE_FLEX], 0, 0);

    Eigen::Vector3f r_hip_base = pelvis + Eigen::Vector3f(PELVIS_WIDTH, 0, 0);
    Eigen::Vector3f r_knee = calculateJointPosition(r_hip_base, Eigen::Vector3f(0, THIGH_LEN, 0), genes[R_HIP_FLEX], genes[R_HIP_ABD], 0);
    Eigen::Vector3f r_ankle = calculateJointPosition(r_knee, Eigen::Vector3f(0, CALF_LEN, 0), genes[R_KNEE_FLEX], 0, 0);

    // [2D 투영]
    Eigen::Matrix3f K = getCameraIntrinsics();
    auto project = [&](const Eigen::Vector3f& v3d) -> Eigen::Vector2f {
        Eigen::Vector3f p = K * v3d; return {p.x() / p.z(), p.y() / p.z()};
    };

    // [오차 계산] (어깨, 팔꿈치, 손목, 고관절, 무릎, 발목 = 총 12개 포인트)
    float error = 0.0f;
    error += (project(l_sh_base) - target2D[0]).norm() + (project(r_sh_base) - target2D[1]).norm();
    error += (project(l_elbow) - target2D[2]).norm() + (project(r_elbow) - target2D[3]).norm();
    error += (project(l_wrist) - target2D[4]).norm() + (project(r_wrist) - target2D[5]).norm();
    error += (project(l_hip_base) - target2D[6]).norm() + (project(r_hip_base) - target2D[7]).norm();
    error += (project(l_knee) - target2D[8]).norm() + (project(r_knee) - target2D[9]).norm();
    error += (project(l_ankle) - target2D[10]).norm() + (project(r_ankle) - target2D[11]).norm();

    // [해부학적 페널티]
    if (genes[L_EL_FLEX] < 0 || genes[L_EL_FLEX] > static_cast<float>(M_PI)) error += 99999.0f;
    if (genes[R_EL_FLEX] < 0 || genes[R_EL_FLEX] > static_cast<float>(M_PI)) error += 99999.0f;
    if (genes[L_KNEE_FLEX] < 0 || genes[L_KNEE_FLEX] > static_cast<float>(M_PI)) error += 99999.0f;
    if (genes[R_KNEE_FLEX] < 0 || genes[R_KNEE_FLEX] > static_cast<float>(M_PI)) error += 99999.0f;

    return error;
}

struct Individual {
    std::array<float, NUM_GENES> genes;
    float fitness;
};

// 4. JNI 통신 함수
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_strapxml_HumanoidOptimizer_runOptimization(
        JNIEnv* env, jobject /* this */, jfloatArray landmarks2D) {

    jfloat* data = env->GetFloatArrayElements(landmarks2D, nullptr);

    // MediaPipe 좌표 추출 (상체 6개 + 하체 6개 = 12개)
    std::vector<Eigen::Vector2f> target2D;
    int indices[] = {11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28};
    for (int idx : indices) {
        target2D.emplace_back(data[idx * 2] * 1080.0f, data[idx * 2 + 1] * 1920.0f);
    }
    env->ReleaseFloatArrayElements(landmarks2D, data, 0);

    const int POPULATION_SIZE = 150;
    const int MAX_GENERATION = 50;
    const float SELECTION_RATIO = 0.2f;
    const float SHRINK_FACTOR = 0.85f;

    int eliteCount = static_cast<int>(static_cast<float>(POPULATION_SIZE) * SELECTION_RATIO);

    std::array<float, NUM_GENES> means = {0.0f};
    std::array<float, NUM_GENES> stds;
    std::fill(stds.begin(), stds.end(), static_cast<float>(M_PI) / 2.0f);

    std::random_device rd; std::mt19937 gen(rd());
    Individual bestOverall; bestOverall.fitness = 999999.0f;

    for (int genIdx = 0; genIdx < MAX_GENERATION; ++genIdx) {
        std::vector<Individual> population(POPULATION_SIZE);
        std::vector<std::normal_distribution<float>> dists;
        for (int j = 0; j < NUM_GENES; ++j) dists.emplace_back(means[j], stds[j]);

        for (int i = 0; i < POPULATION_SIZE; ++i) {
            for (int j = 0; j < NUM_GENES; ++j) population[i].genes[j] = dists[j](gen);
            population[i].fitness = calculateLoss(target2D, population[i].genes);
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
        }
    }

    std::vector<float> resultAngles(NUM_GENES);
    for(int i = 0; i < NUM_GENES; ++i) resultAngles[i] = bestOverall.genes[i] * 180.0f / static_cast<float>(M_PI);

    jfloatArray resultArray = env->NewFloatArray(static_cast<jsize>(resultAngles.size()));
    env->SetFloatArrayRegion(resultArray, 0, static_cast<jsize>(resultAngles.size()), resultAngles.data());

    return resultArray;
}