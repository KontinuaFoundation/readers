from django.core.management.base import BaseCommand, CommandError
from core.models import Collection


class Command(BaseCommand):
    help = "Deletes all collections and in turn all workbooks. It will remove all pdfs from S3 also."

    def handle(self, *args, **options):
        Collection.objects.all().delete()
        self.stdout.write(self.style.SUCCESS('Successfully deleted all collections'))