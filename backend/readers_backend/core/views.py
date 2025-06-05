from rest_framework import mixins
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.decorators import action
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from core.throttles import FeedbackThrottle
from .utils import send_feedback_email
from rest_framework.viewsets import GenericViewSet
from django.utils import timezone
from django.conf import settings
from rest_framework.exceptions import ValidationError

from core.models import Collection, Workbook, Feedback
from core.serializers import (
    CollectionListSerializer,
    CollectionRetrieveQueryParamsSerializer,
    WorkbookCreateSerializer,
    CollectionCreateSerializer,
    CollectionRetrieveSerializer,
    WorkbookRetrieveSerializer,
    FeedbackSerializer,
)


# TODO:
# Let's use an openapi schema/library like drf-yasg to generate the API documentation.
# Then lets display that here.
class RootAPIView(APIView):
    permission_classes = [AllowAny]
    serializer_class = None

    def get(self, request):
        return Response({"message": f"Readers API v{settings.API_VERSION}"})


class DestroyAuthTokenView(APIView):
    permission_classes = [IsAuthenticated]
    serializer_class = None

    def delete(self, request):
        Token.objects.filter(user=request.user).delete()
        return Response(
            {"message": "Token deleted."}, status=status.HTTP_204_NO_CONTENT
        )


class CollectionViewSet(
    GenericViewSet,
    mixins.CreateModelMixin,
    mixins.DestroyModelMixin,
    mixins.ListModelMixin,
    mixins.RetrieveModelMixin,
):

    def get_serializer_class(self):
        if self.action == "create":
            return CollectionCreateSerializer
        elif self.action == "list":
            return CollectionListSerializer
        elif self.action == "retrieve":
            return CollectionRetrieveSerializer
        # TODO: Consider just returning the entire collection rather than the list representation...
        elif self.action == "latest":
            return CollectionListSerializer

        # drf-spectacular requires a default serializer.
        return CollectionListSerializer

    def get_permissions(self):
        if self.action in ["list", "retrieve", "latest"]:
            return []
        return [IsAuthenticated()]

    def get_queryset(self):
        # drf-spectacular compatibility.
        if getattr(self, "swagger_fake_view", False):
            return Collection.objects.none()

        queryset = Collection.objects.all()

        # Unauthenticated users should never have access to unreleased collections.
        if not self.request.user.is_authenticated:
            queryset = queryset.filter(is_released=True)

        # Query param filtering only applies for listing collections or retrieving the latest collection.
        if not self.action in ["list", "latest"]:
            return queryset

        params = self.request.query_params

        query_params_serializer = CollectionRetrieveQueryParamsSerializer(data=params)

        if not query_params_serializer.is_valid():
            raise ValidationError(query_params_serializer.errors)

        major_version = query_params_serializer.validated_data.get(
            "major_version", None
        )
        minor_version = query_params_serializer.validated_data.get(
            "minor_version", None
        )
        localization = query_params_serializer.validated_data.get("localization", None)
        is_released = query_params_serializer.validated_data.get("is_released", None)

        if major_version is not None:
            queryset = queryset.filter(major_version=major_version)

        if minor_version is not None:
            queryset = queryset.filter(minor_version=minor_version)

        if localization:
            queryset = queryset.filter(localization=localization)

        if is_released is not None:
            queryset = queryset.filter(is_released=is_released)

        return queryset

    @action(detail=True, methods=["patch"])
    def release(self, request, pk=None):
        collection = self.get_object()
        collection.is_released = True
        collection.save()
        return Response({"message": "Collection released."}, status=status.HTTP_200_OK)

    @action(detail=True, methods=["patch"])
    def unrelease(self, request, pk=None):
        collection = self.get_object()
        collection.is_released = False
        collection.save()
        return Response(
            {"message": "Collection un-released."}, status=status.HTTP_200_OK
        )

    @action(detail=False, methods=["get"])
    def latest(self, request):
        # TODO: Lets make this return the collection retrieve serializer at some point.
        # More specifically, is there a reason to make the client two two requests to get the chapters for the latest collection?
        queryset = self.get_queryset()

        try:
            latest = queryset.latest()
        except Collection.DoesNotExist:
            return Response(
                {"message": "No collections found."}, status=status.HTTP_404_NOT_FOUND
            )

        serializer = self.get_serializer(latest)
        return Response(serializer.data)


class WorkbookViewSet(
    GenericViewSet,
    mixins.CreateModelMixin,
    mixins.DestroyModelMixin,
    mixins.RetrieveModelMixin,
):
    queryset = Workbook.objects.all()

    def get_queryset(self):
        # drf-spectacular compatibility.
        if getattr(self, "swagger_fake_view", False):
            return Workbook.objects.none()

        queryset = super().get_queryset()

        # Only users can access unreleased workbooks.
        if not self.request.user.is_authenticated:
            queryset = queryset.filter(collection__is_released=True)

        return queryset

    def get_serializer_class(self):
        if self.action == "create":
            return WorkbookCreateSerializer
        if self.action == "retrieve":
            return WorkbookRetrieveSerializer

        # drf-spectacular requires a default serializer.
        return WorkbookRetrieveSerializer

    def get_permissions(self):
        if self.action in ["list", "retrieve"]:
            return []

        return [IsAuthenticated()]


# TODO:
# Currently we use google app specific password to send emails which limits the number of emails we can send.
# This endpoint also does not have a strict rate limit per request.
# This should be addressed in the future...
class FeedbackView(APIView):
    """
    API endpoint that allows users to submit feedback.
    """

    permission_classes = [AllowAny]  # Allow unauthenticated users to submit feedback
    serializer_class = FeedbackSerializer
    throttle_classes = [FeedbackThrottle]

    def post(self, request):
        try:
            serializer = FeedbackSerializer(data=request.data)

            if serializer.is_valid():
                # Save the feedback to the database
                feedback = serializer.save()

                # Send email notification
                try:
                    email_sent = send_feedback_email(feedback)
                except Exception as e:
                    # Don't let email failures break the feedback submission
                    print(f"Failed to send feedback email: {e}")
                    email_sent = False

                return Response(
                    {
                        "message": "Feedback submitted successfully",
                        "email_sent": email_sent,
                    },
                    status=status.HTTP_201_CREATED,
                )

            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        except Exception as e:
            # Log the full error for debugging
            print(f"Error in FeedbackView: {str(e)}")
            import traceback

            print(traceback.format_exc())

            return Response(
                {"error": "Internal server error"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
