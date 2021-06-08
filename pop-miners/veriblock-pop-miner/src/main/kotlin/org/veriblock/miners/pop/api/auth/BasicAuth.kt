package org.veriblock.miners.pop.api.auth

import com.papsign.ktor.openapigen.APIException
import com.papsign.ktor.openapigen.model.security.APIKeyLocation
import com.papsign.ktor.openapigen.model.security.HttpSecurityScheme
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeType
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.throws
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.util.pipeline.*
import org.veriblock.miners.pop.api.controller.BadPrincipalException

inline fun<T> NormalOpenAPIRoute.basicAuth(
    authProvider: AuthProvider<T>,
    crossinline route: OpenAPIAuthenticatedRoute<T>.()->Unit = {}
): OpenAPIAuthenticatedRoute<T> {
    return authProvider.apply(this).apply {
        route()
    }
}

class BasicProvider(
    val authName: String,
    val optional: Boolean
) : AuthProvider<UserIdPrincipal> {
    override suspend fun getAuth(pipeline: PipelineContext<Unit, ApplicationCall>): UserIdPrincipal {
        return pipeline.context.authentication.principal() ?: throw RuntimeException("No BasicAuthPrincipal")
    }

    override fun apply(route: NormalOpenAPIRoute): OpenAPIAuthenticatedRoute<UserIdPrincipal> =
        OpenAPIAuthenticatedRoute(
            route.ktorRoute.authenticate(authName, optional = optional) {},
            route.provider.child(),
            this
        ).throws(APIException.apiException<BadPrincipalException>(HttpStatusCode.Unauthorized))

    override val security: Iterable<Iterable<AuthProvider.Security<*>>> =
        listOf(
            listOf(
                AuthProvider.Security(
                    SecuritySchemeModel(
                        SecuritySchemeType.http,
                        authName,
                        APIKeyLocation.header,
                        HttpSecurityScheme.basic,
                        null,
                        null
                    ),
                    emptyList<Scopes>()
                )
            )
        )
}
