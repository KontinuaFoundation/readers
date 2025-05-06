from django.views.generic import TemplateView
from core.models import Collection


class IndexView(TemplateView):
    '''
    Renders a grid of the latest en-US collection's workbooks.
    '''

    template_name = "index.html"

    def get_context_data(self, **kwargs):

        try:
            latest = (
                Collection.objects.filter(is_released=True, localization="en-US")
                .prefetch_related("workbooks")
                .latest()
            )
        except Collection.DoesNotExist:
            return {
                "latest_us_collection": None,
            }

        return {
            "latest_us_collection": latest,
        }