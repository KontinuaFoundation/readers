import logging
from django.http import HttpResponse


logger = logging.getLogger("readers.request_response")


class RequestLoggerMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # Code to be executed for each request before
        # the view (and later middleware) are called.

        logger.info(f"Request: {request.method} {request.path}")

        response = self.get_response(request)

        logger.info(f"Response: {response.status_code}")

        # Code to be executed for each request/response after
        # the view is called.

        return response
