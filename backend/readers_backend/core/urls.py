"""
URL configuration for readers_backend project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/5.1/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""

from django.urls import path
from rest_framework.authtoken.views import obtain_auth_token
from rest_framework.routers import DefaultRouter
from core.views import (
    FeedbackView,
    DestroyAuthTokenView,
    CollectionViewSet,
    RootAPIView,
    WorkbookViewSet,
)
from django.contrib import admin

from core.views import (
    FeedbackView,
    DestroyAuthTokenView,
    CollectionViewSet,
    WorkbookViewSet,
)

router = DefaultRouter()
router.register("api/collections", CollectionViewSet, basename="collection")
router.register("api/workbooks", WorkbookViewSet, basename="workbook")

urlpatterns = [
    path("api/", RootAPIView.as_view(), name="root"),
    path("api/token/", obtain_auth_token, name="token-obtain"),
    path("api/token/destroy/", DestroyAuthTokenView.as_view(), name="token-destroy"),
    path("api/feedback/", FeedbackView.as_view(), name="feedback"),
    path("management/", admin.site.urls),
]

urlpatterns += router.urls
