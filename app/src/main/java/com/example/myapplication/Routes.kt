package com.example.myapplication

sealed class Route(val route: String) {

    // 인증
    object Login : Route("login")
    object Register : Route("register")

    // 메인
    object Main : Route("main")

    // 기능
    object Chatbot : Route("chatbot")
    object Record : Route("record")
    object Library : Route("library")
    object Calendar : Route("calendar")
    object Alarm : Route("alarm")
    object Community : Route("community")
    object Routine : Route("routine")

    // 프로필
    object Profile : Route("profile")


}