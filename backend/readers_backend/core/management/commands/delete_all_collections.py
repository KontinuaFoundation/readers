from django.core.management.base import BaseCommand, CommandError
from core.models import Collection


class Command(BaseCommand):
    help = "Closes the specified poll for voting"


    def handle(self, *args, **options):
        Collection.objects.all().delete()
        self.stdout.write(self.style.SUCCESS('Successfully deleted all collections'))