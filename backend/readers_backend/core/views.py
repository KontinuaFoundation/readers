from rest_framework import mixins
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.viewsets import GenericViewSet

from core.models import Collection, Workbook
from core.serializers import CollectionListSerializer, WorkbookCreateSerializer, CollectionCreateSerializer, \
    CollectionRetrieveSerializer, WorkbookRetrieveSerializer


class DestroyAuthTokenView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request):
        Token.objects.filter(user=request.user).delete()
        return Response({"message": "Token deleted."}, status=status.HTTP_204_NO_CONTENT)

class CollectionViewSet(GenericViewSet, mixins.CreateModelMixin, mixins.DestroyModelMixin, mixins.ListModelMixin, mixins.RetrieveModelMixin):

    def get_serializer_class(self):
        if self.action == 'create':
            return CollectionCreateSerializer
        elif self.action == 'list':
            return CollectionListSerializer
        elif self.action == 'retrieve':
            return CollectionRetrieveSerializer
        return None

    def get_permissions(self):
        if self.action in ['list', 'retrieve']:
            return []
        return [IsAuthenticated()]

    def get_queryset(self):
        queryset = Collection.objects.all()

        # Query param filtering only applies for listing collections.
        if self.action != 'list':
            return queryset

        params = self.request.query_params

        major_version = params.get('major_version')
        minor_version = params.get('minor_version')
        localization = params.get('localization')
        is_released = params.get('is_released')

        if major_version is not None:
            queryset = queryset.filter(major_version=major_version)

        if minor_version is not None:
            queryset = queryset.filter(minor_version=minor_version)

        if localization:
            queryset = queryset.filter(localization=localization)

        if is_released is not None:
            is_released_bool = is_released.lower() == 'true'
            queryset = queryset.filter(is_released=is_released_bool)

        return queryset

class WorkbookViewSet(GenericViewSet, mixins.CreateModelMixin, mixins.DestroyModelMixin, mixins.RetrieveModelMixin):
    queryset = Workbook.objects.all()

    def get_serializer_class(self):
        if self.action == 'create':
            return WorkbookCreateSerializer
        if self.action == 'retrieve':
            return WorkbookRetrieveSerializer
        return None

    def get_permissions(self):
        if self.action in ['list', 'retrieve']:
            return []

        return [IsAuthenticated()]