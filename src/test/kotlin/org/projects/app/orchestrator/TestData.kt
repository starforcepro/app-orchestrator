package org.projects.app.orchestrator

import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.encoding.Base64


fun getArchivedAppBase64(): String {
    return Base64.encode(expressJsFileContent.toByteArray().toZipByteArray())
}

val expressJsFileContent = $$"""
            // AWS Lambda handler equivalent for the former Express app
            // Expected behavior:
            // - GET    /                              -> 200, body: "success"
            // - GET    /notFound                      -> 400, body: "bad request text"
            // - GET    /testProxyingWithQueryParams   -> 200, body: query param `text`
            // - POST   /testProxyingWithBody          -> 200, body: json body field `text`
            
            exports.handler = async (event) => {
              const path = (event && (event.rawPath || event.path)) || '/';
              const method = ((event && (event.requestContext && event.requestContext.http && event.requestContext.http.method)) || event.httpMethod || 'GET').toUpperCase();

              const ok = (body) => ({ statusCode: 200, headers: { 'Content-Type': 'text/plain' }, body: String(body ?? '') });
              const badRequest = (body) => ({ statusCode: 400, headers: { 'Content-Type': 'text/plain' }, body: String(body ?? '') });
              const notFound = () => ({ statusCode: 404, headers: { 'Content-Type': 'text/plain' }, body: 'Not Found' });
              const getQuery = (name) => {
                const qp = event && (event.queryStringParameters || event.multiValueQueryStringParameters);
                if (!qp) return undefined;
                return (qp[name] && (Array.isArray(qp[name]) ? qp[name][0] : qp[name])) || undefined;
              };
              const getJsonBody = () => {
                if (!event || event.body == null) return {};
                try {
                  return typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
                } catch (e) {
                  return {};
                }
              };
              
              if (path === '/' && method === 'GET') {
                return ok('success');
              }
              
              if (path === '/notFound' && method === 'GET') {
                return badRequest('bad request text');
              }
              
              if (path === '/testProxyingWithQueryParams' && method === 'GET') {
                const text = getQuery('text') || '';
                return ok(`${text}`);
              }
              
              if (path === '/testProxyingWithBody' && method === 'POST') {
                const body = getJsonBody();
                const text = (body && body.text) || '';
                return ok(`${text}`);
              }
              
              return notFound();
            };
        """.trimIndent()

fun ByteArray.toZipByteArray(): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    ZipOutputStream(byteArrayOutputStream).use { zipOutputStream ->
        val entry = ZipEntry("index.js")
        zipOutputStream.putNextEntry(entry)
        zipOutputStream.write(this)
        zipOutputStream.closeEntry()
    }
    return byteArrayOutputStream.toByteArray()
}

fun uniqueName(): String = UUID.randomUUID().toString()