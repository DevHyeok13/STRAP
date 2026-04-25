#include <jni.h>
#include <vector>

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_strapxml_HumanoidOptimizer_runOptimization(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray landmarks2D) {

    // 1. Kotlin에서 받아온 2D 좌표(FloatArray)를 C++ 배열로 변환
    jsize length = env->GetArrayLength(landmarks2D);
    jfloat* data = env->GetFloatArrayElements(landmarks2D, 0);

    // ---------------------------------------------------------
    // TODO: 여기에 논문의 uDEAS 최적화 알고리즘과 행렬 연산이 들어갑니다.
    // 일단 연결이 잘 되었는지 확인하기 위해 가짜 계산 결과를 만들어 보겠습니다.
    // (예: 입력된 2D 좌표 개수 출력 및 가짜 3D 각도 3개 반환)
    // ---------------------------------------------------------

    // 가짜 최적화 결과 각도 (예: 어깨, 요추, 골반 각도)
    std::vector<float> resultAngles = {90.0f, 45.5f, 180.0f};

    // 메모리 누수 방지를 위해 사용이 끝난 배열 해제 (필수!)
    env->ReleaseFloatArrayElements(landmarks2D, data, 0);

    // 2. C++에서 계산된 결과(vector)를 다시 Kotlin 배열(FloatArray)로 포장
    jfloatArray resultArray = env->NewFloatArray(resultAngles.size());
    env->SetFloatArrayRegion(resultArray, 0, resultAngles.size(), resultAngles.data());

    // 코틀린으로 결과 발사!
    return resultArray;
}