from django.db.models.signals import post_delete, pre_delete
from django.dispatch import receiver
from core.models import Workbook


# By default, django does not delete file when objects with a file field are deleted...
@receiver(pre_delete, sender=Workbook)
def delete_workbook_pdf_individual(sender, instance, **kwargs):
    """Handle individual workbook deletes"""
    if instance.pdf:
        instance.pdf.delete(False)
