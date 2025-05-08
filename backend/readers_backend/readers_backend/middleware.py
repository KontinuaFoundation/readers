from django.http import HttpResponse


# We dont know the local ip of the app runner instance.
# So when health check is called on the local ip, we need to return a 200 response.
# Since we dont know the local ip, we need to return 200 ok for all requests regardless of the host origin.
# By default, django will return a 400 error for all requests that are not in the ALLOWED_HOSTS list.
# Placing this middleware in front of common middleware will bypass this check on this single endpoint!


class HealthCheckMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if request.path == "/health" or request.path == "/health/":
            return HttpResponse("Health check passed!", status=200)

        return self.get_response(request)
