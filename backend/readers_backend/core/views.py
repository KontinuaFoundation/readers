from rest_framework import mixins
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.decorators import action
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView
from .utils import send_feedback_email
from rest_framework.viewsets import GenericViewSet
from django.utils import timezone
from django.conf import settings

from core.models import Collection, Workbook, Feedback
from core.serializers import (
    CollectionListSerializer,
    WorkbookCreateSerializer,
    CollectionCreateSerializer,
    CollectionRetrieveSerializer,
    WorkbookRetrieveSerializer,
    FeedbackSerializer,
)


class DestroyAuthTokenView(APIView):
    permission_classes = [IsAuthenticated]

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
        return None

    def get_permissions(self):
        if self.action in ["list", "retrieve", "latest"]:
            return []
        return [IsAuthenticated()]

    def get_queryset(self):
        queryset = Collection.objects.all()

        # Unauthenticated users should never have access to unreleased collections.
        if not self.request.user.is_authenticated:
            queryset = queryset.filter(is_released=True)

        # Query param filtering only applies for listing collections or retrieving the latest collection.
        if not self.action in ["list", "latest"]:
            return queryset

        params = self.request.query_params

        major_version = params.get("major_version")
        minor_version = params.get("minor_version")
        localization = params.get("localization")
        is_released = params.get("is_released")

        if major_version is not None:
            queryset = queryset.filter(major_version=major_version)

        if minor_version is not None:
            queryset = queryset.filter(minor_version=minor_version)

        if localization:
            queryset = queryset.filter(localization=localization)

        # We've already filtered for released collections if the user is not authenticated so doesn't matter if we apply this here.
        if is_released is not None:
            is_released_bool = is_released.lower() == "true"
            queryset = queryset.filter(is_released=is_released_bool)

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

    def get_serializer_class(self):
        if self.action == "create":
            return WorkbookCreateSerializer
        if self.action == "retrieve":
            return WorkbookRetrieveSerializer
        return None

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

    def post(self, request):
        serializer = FeedbackSerializer(data=request.data)

        if serializer.is_valid():
            # Save the feedback to the database
            if not settings.DEBUG:
                feedback = serializer.save()
            else:
                feedback = Feedback(**serializer.validated_data)
                feedback.created_at = timezone.now()  # Only need this when not saving
                print("DEBUG mode: Feedback not saved to database")

            # Send email notification without saving to DB
            email_sent = send_feedback_email(feedback)

            return Response(
                {
                    "message": "Feedback submitted successfully",
                    "email_sent": email_sent,
                },
                status=status.HTTP_201_CREATED,
            )

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
