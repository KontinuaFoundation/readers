from django.core.mail import EmailMessage
from django.conf import settings
from django.template.loader import render_to_string

def send_feedback_email(feedback):
    """
    Send feedback from a user to the app's own email address.

    Args:
        feedback: Feedback object with all required information

    Returns:
        bool: Whether the email was sent successfully
    """
    # Use version info directly from the feedback object
    subject = f"User Feedback: Workbook {feedback.workbook.number} (v{feedback.major_version}.{feedback.minor_version} {feedback.localization})"

    # Prepare context for the email
    context = {
        "feedback": feedback,
        "workbook_number": feedback.workbook.number,
        "major_version": feedback.major_version,
        "minor_version": feedback.minor_version,
        "localization": feedback.localization,
        "page_number": feedback.page_number,
        "chapter_number": feedback.chapter_number,
        "description": feedback.description,
        "user_email": feedback.user_email,
        "date_submitted": feedback.created_at.strftime("%Y-%m-%d %H:%M:%S"),
    }

    # Create email body
    html_message = render_to_string("emails/feedback.html", context)

    # Create email
    email = EmailMessage(
        subject=subject,
        body=html_message,
        from_email=settings.DEFAULT_FROM_EMAIL,
        to=[settings.FEEDBACK_EMAIL],
    )

    email.content_subtype = "html"  # Main content is now HTML

    try:
        email.send()
        return True
    except Exception as e:
        print(f"Error sending feedback email: {e}")
        return False
