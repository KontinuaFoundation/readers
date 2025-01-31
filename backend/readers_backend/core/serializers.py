from rest_framework import serializers
from .models import Collection, Workbook, Feedback

class CollectionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Collection
        fields = '__all__'