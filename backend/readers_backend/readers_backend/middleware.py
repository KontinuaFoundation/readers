from datetime import datetime
import uuid
import json
import logging
from django.http import HttpResponse
import traceback


request_logger = logging.getLogger("readers.requests")
response_logger = logging.getLogger("readers.responses")
exception_logger = logging.getLogger("readers.exceptions")


class LoggingMiddleware:
    """
    This middleware is responsible for logging the readers backend.
    More specifically, it logs requests, responses, and any unhandeled exceptions.
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def remove_sensitive_values_from_headers(self, headers: dict):
        if headers is None:
            return None

        if "Authorization" in headers:
            headers["Authorization"] = "********"

        return headers

    def remove_sensitive_values_from_body(self, body: dict):

        if body is None:
            return None

        if "password" in body:
            body["password"] = "********"

        return body

    def __call__(self, request):

        try:
            body = dict(request.body)
            headers = dict(request.headers)

            request_time = datetime.now()

            request.log_id = str(uuid.uuid4())

            request_message = {
                "request_id": request.log_id,
                "time": request_time.isoformat(),
                "method": request.method,
                "path": request.path,
                "query_params": request.GET.dict(),
                "body": self.remove_sensitive_values_from_body(body),
                "headers": self.remove_sensitive_values_from_headers(headers),
            }

            request_logger.info("Request: " + json.dumps(request_message, indent=2))

        except Exception as e:
            # Logging should never cause request to fail...
            exception_logger.error("Error logging request: " + str(e))

        response = self.get_response(request)

        try:
            response_message = {
                "request_id": request.log_id,
                "response_time": (datetime.now() - request_time).total_seconds(),
                "status_code": response.status_code,
            }

            if response.status_code >= 400:
                try:
                    response_message["content"] = json.loads(response.content)
                except json.JSONDecodeError:
                    response_message["content"] = "Unable to parse response content."

            response_logger.info("Response: " + json.dumps(response_message, indent=2))

        except Exception as e:
            # Logging should never cause request to fail...
            exception_logger.error("Error logging response: " + str(e))

        return response

    def process_exception(self, request, exception):
        # This is a catch-all for any unhandled exceptions.
        # We log them here, but we don't want to return a response to the client.

        exception_logger.error(
            f"Request ID: {request.log_id}\n"
            f"Exception: {str(exception)}\n"
            f"Stack trace:\n{traceback.format_exc()}"
        )

        # TODO: Some sort of notifcation to the dev team.

        return None
