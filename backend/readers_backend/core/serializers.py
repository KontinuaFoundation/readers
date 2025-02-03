from rest_framework import serializers

from .models import Collection


class CollectionWriteSerializer(serializers.ModelSerializer):
    class Meta:
        model = Collection
        fields = '__all__'
        read_only_fields = ['creation_date', 'is_released']
