from rest_framework import mixins
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.viewsets import GenericViewSet

from core.models import Collection
from core.serializers import CollectionWriteSerializer


class DestroyAuthTokenView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request):
        Token.objects.filter(user=request.user).delete()
        return Response({"message": "Token deleted."}, status=status.HTTP_204_NO_CONTENT)


class CollectionViewSet(GenericViewSet, mixins.CreateModelMixin):
    queryset = Collection.objects.all()

    def get_permissions(self):
        return [IsAuthenticated()]

    def get_serializer_class(self):
        return CollectionWriteSerializer
