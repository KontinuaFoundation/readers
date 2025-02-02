from rest_framework.response import Response
from rest_framework.authtoken.models import Token
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

class DestroyAuthTokenView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request):
        Token.objects.filter(user=request.user).delete()
        return Response({"message": "Token deleted."}, status=status.HTTP_204_NO_CONTENT)