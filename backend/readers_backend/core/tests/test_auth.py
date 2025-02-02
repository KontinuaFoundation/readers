from django.contrib.auth.models import User
from django.urls import reverse
from rest_framework.test import APITestCase
from rest_framework import status
from rest_framework.authtoken.models import Token

class TokenRetrievalTestCase(APITestCase):

    def test_obtain_auth_token(self):
        """Test if a user can obtain an authentication token from /api/token endpoint."""

        user = User.objects.create_user(username="testuser", password="testpass123")

        url = reverse('token-obtain')

        data = {"username":"testuser", "password": "testpass123"}
        response = self.client.post(url, data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=f'Token response not 200 OK:\n {response.data}')
        self.assertIn("token", response.data, msg="Token not in response:\n{response.data}")

    def test_destroy_auth_token(self):
        """Test if a user can destroy an authentication token from /api/token/destroy endpoint."""

        user = User.objects.create_user(username="testuser", password="testpass123")

        token = Token.objects.create(user=user)

        # Token will be cleared between test cases!
        self.client.credentials(HTTP_AUTHORIZATION=f"Token {token.key}")

        self.assertTrue(Token.objects.filter(user=user).exists(), "Token was not created properly.")

        response = self.client.delete("/api/token/destroy/")

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT, msg=f"Token response not 204 NO CONTENT:\n {response.data}")

        self.assertFalse(Token.objects.filter(user=user).exists(), "Token was not deleted successfully.")