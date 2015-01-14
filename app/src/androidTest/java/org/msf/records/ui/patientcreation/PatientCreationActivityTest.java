package org.msf.records.ui.patientcreation;

import org.msf.records.R;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.ui.userlogin.UserLoginActivity;

import java.util.Date;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.RootMatchers.isDialog;
import static com.google.android.apps.common.testing.ui.espresso.matcher.RootMatchers.withDecorView;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.msf.records.ui.matchers.AppPatientMatchers.isPatientWithId;

public class PatientCreationActivityTest extends FunctionalTestCase {

    public void setUp() throws Exception {
        super.setUp();
        // Go to PatientCreationActivity
        onView(withText("Guest User")).perform(click());
        onView(withId(R.id.action_add)).perform(click());
        onView(withText("New Patient")).check(matches(isDisplayed()));
    }

    /** Populates all the fields on the New Patient screen, except location. */
    public void populateNewPatientFieldsExceptLocation(String id) {
        onView(withId(R.id.patient_creation_text_patient_id)).perform(typeText(id));
        onView(withId(R.id.patient_creation_text_patient_given_name)).perform(typeText("Given" + id));
        onView(withId(R.id.patient_creation_text_patient_family_name)).perform(typeText("Family" + id));
        onView(withId(R.id.patient_creation_text_age)).perform(typeText(id));
        onView(withId(R.id.patient_creation_radiogroup_age_units_years)).perform(click());
        onView(withId(R.id.patient_creation_radiogroup_age_units_months)).perform(click());
        onView(withId(R.id.patient_creation_radiogroup_age_sex_male)).perform(click());
        onView(withId(R.id.patient_creation_radiogroup_age_sex_female)).perform(click());
    }

    /** Checks that a given patient appears in the patient list, and clicks it. */
    public void clickPatientWithIdInPatientList(String id) {
        onData(isPatientWithId(equalTo(id)))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
    }

    /** Tests adding a new patient with a location. */
    public void testNewPatientWithLocation() {
        String id = "test" + new Date().getTime() % 1000;
        populateNewPatientFieldsExceptLocation(id);
        onView(withId(R.id.patient_creation_button_change_location)).perform(click());
        onView(withText("S1")).perform(click());
        onView(withText("Create")).perform(click());

        // Navigate to patient list for test S1
        onView(withText("S1")).perform(click());

        // The new patient should be visible in the list
        clickPatientWithIdInPatientList(id);
    }

    /** Tests adding a new patient with no location. */
    public void testNewPatientWithoutLocation() {
        String id = "test" + new Date().getTime() % 1000;
        populateNewPatientFieldsExceptLocation(id);
        onView(withText("Create")).perform(click());
        clickPatientWithIdInPatientList(id);
    }

    /** Tests that a confirmation prompt appears upon cancelling the form. */
    public void testNewPatientCancel() {
        onView(withId(R.id.patient_creation_text_patient_id)).perform(typeText("xyz"));
        pressBack(); // close the keyboard

        // Attempting to back out of the activity should trigger a prompt
        pressBack();
        onView(withText(containsString("Discard"))).check(matches(isDisplayed()));

        // Dismiss the prompt
        onView(withText("Yes")).perform(click());
    }
}
