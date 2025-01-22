package org.example.bestme.exception

import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.function.client.WebClientResponseException

@RestControllerAdvice
class GlobalExceptionHandler {

    // 서버 내부 오류 처리
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Void?>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다: ${ex.message}"))
    }

    // 잘못된 요청 처리
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Void?>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "잘못된 요청입니다: ${ex.message}"))
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ApiResponse<Void?>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다: ${ex.message}"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Void?>> {
        val errorMessage = ex.bindingResult
            .fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
            .ifEmpty { "잘못된 입력입니다." }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST, errorMessage))
    }

    // WebClient 호출 실패 처리
    @ExceptionHandler(WebClientResponseException::class)
    fun handleWebClientResponseException(ex: WebClientResponseException): ResponseEntity<ApiResponse<Void?>> {
        // 인증 키 만료(401) 오류 처리
        if (ex.statusCode == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "인증 키가 유효하지 않거나 만료되었습니다. 새로운 키로 다시 시도해주세요."))
        }

        // 일반 WebClient 오류 처리
        var errorMessage = "API 호출 실패: ${ex.message}"
        ex.responseBodyAsString?.let {
            errorMessage += " - 상세 오류: $it"
        }

        // HttpStatus 변환
        val httpStatus = HttpStatus.valueOf(ex.statusCode.value())

        return ResponseEntity.status(httpStatus)
            .body(ApiResponse.error(httpStatus, errorMessage))
    }
}
