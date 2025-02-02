from django.contrib.auth.models import User
from django.urls import reverse
from rest_framework.test import APITestCase
from rest_framework import status

class TokenRetrievalTestCase(APITestCase):

    def test_obtain_auth_token(self):
        """Test if a user can obtain an authentication token from /api/token endpoint."""

        user = User.objects.create_user(username="testuser", password="testpass123")

        url = reverse('token-retrieve')

        data = {"username":"testuser", "password": "testpass123"}
        response = self.client.post(url, data, format="json")

        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=f'Token response not 200 OK:\n {response.data}')
        self.assertIn("token", response.data, msg="Token not in response:\n{response.data}")