package com.aifordev.contract

import com.fasterxml.jackson.databind.ObjectMapper
import org.openapi4j.operation.validator.model.Request
import org.openapi4j.operation.validator.model.Response
import org.openapi4j.operation.validator.model.impl.Body
import org.openapi4j.operation.validator.model.impl.DefaultResponse
import org.openapi4j.operation.validator.validation.OperationValidator
import org.openapi4j.parser.OpenApi3Parser
import org.openapi4j.parser.model.v3.OpenApi3
import org.openapi4j.parser.model.v3.Path
import org.openapi4j.schema.validator.ValidationData
import org.springframework.core.io.ClassPathResource

object ContractValidator {
    private val spec: OpenApi3 by lazy {
        val url = ClassPathResource("openapi.json").url
        OpenApi3Parser().parse(url, false)
    }

    private val mapper = ObjectMapper()

    fun validateResponse(
        path: String,
        method: Request.Method,
        statusCode: Int,
        body: String? = null,
    ) {
        val specPath =
            spec.getPath(path)
                ?: throw IllegalArgumentException("No path found for $path in OpenAPI spec")

        val operation =
            getOperation(specPath, method)
                ?: throw IllegalArgumentException("No operation found for $method $path in OpenAPI spec")

        val validator = OperationValidator(spec, specPath, operation)
        val response = buildResponse(statusCode, body)
        val validationData = ValidationData<Void>()
        validator.validateResponse(response, validationData)

        if (!validationData.isValid) {
            val errors = validationData.results().items().joinToString("\n") { it.message() ?: it.toString() }
            throw AssertionError("Contract validation failed for $method $path [statusCode=$statusCode]:\n$errors")
        }
    }

    private fun getOperation(
        specPath: Path,
        method: Request.Method,
    ) = specPath.getOperation(
        when (method) {
            Request.Method.GET -> "get"
            Request.Method.POST -> "post"
            Request.Method.PUT -> "put"
            Request.Method.DELETE -> "delete"
            Request.Method.PATCH -> "patch"
            Request.Method.HEAD -> "head"
            Request.Method.OPTIONS -> "options"
            Request.Method.TRACE -> "trace"
        },
    )

    private fun buildResponse(
        statusCode: Int,
        bodyString: String?,
    ): Response {
        val builder = DefaultResponse.Builder(statusCode)
        if (bodyString != null) {
            val bodyNode = mapper.readTree(bodyString)
            builder.body(Body.from(bodyNode))
            builder.header("Content-Type", "application/json")
        }
        return builder.build()
    }
}
