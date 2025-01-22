package org.example.bestme.controller

import org.example.bestme.exception.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class TestController{

    // 응답 예시
    @GetMapping("/test")
    fun testResponse(): ResponseEntity<ApiResponse<Void?>> {
        return ResponseEntity.ok(ApiResponse.success("Test", null))
    }
}
