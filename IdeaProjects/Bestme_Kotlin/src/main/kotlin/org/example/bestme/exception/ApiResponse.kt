package org.example.bestme.exception

import org.springframework.http.HttpStatus

data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: Int,
    val message: String,
    val data: T?
) {
    companion object {
        // 성공 응답 (기본 메시지)
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(true, HttpStatus.OK.value(), "성공", data)
        }

        // 성공 응답 (커스텀 메시지)
        fun <T> success(message: String, data: T): ApiResponse<T> {
            return ApiResponse(true, HttpStatus.OK.value(), message, data)
        }

        // 성공 응답 (httpStatus 코드 설정 가능)
        fun <T> success(httpStatus: HttpStatus, message: String, data: T): ApiResponse<T> {
            return ApiResponse(true, httpStatus.value(), message, data)
        }

        // 에러 응답
        fun error(httpStatus: HttpStatus): ApiResponse<Void?> {
            return ApiResponse(false, httpStatus.value(), "실패", null)
        }

        fun error(httpStatus: HttpStatus, message: String): ApiResponse<Void?> {
            return ApiResponse(false, httpStatus.value(), message, null)
        }

        fun <T> error(httpStatus: HttpStatus, message: String, data: T?): ApiResponse<T?> {
            return ApiResponse(false, httpStatus.value(), message, data)
        }
    }
}
