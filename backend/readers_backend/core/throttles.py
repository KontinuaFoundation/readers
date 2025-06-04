from rest_framework.throttling import AnonRateThrottle


class FeedbackThrottle(AnonRateThrottle):
    scope = "feedback"
