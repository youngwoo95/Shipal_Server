package com.shipal.shipal.common.exception

/*
*  코틀린에서는 생성자 매개변수와 클래스 프로퍼티를 동시에 선언 가능.
*/
class InvalidInputException (
    val fieldName: String = "",
    message: String ="Invalid Input"
) : RuntimeException(message)