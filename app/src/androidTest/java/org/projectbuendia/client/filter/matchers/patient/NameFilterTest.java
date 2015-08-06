// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.filter.matchers.patient;

import android.test.InstrumentationTestCase;

import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.utils.Utils;

/** Tests for {@link NameFilter}. */
public class NameFilterTest extends InstrumentationTestCase {
    private NameFilter mNameFilter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mNameFilter = new NameFilter();
    }

    /** Tests that name matching works when a matching full name is provided. */
    public void testMatches_exactMatchOnFullName() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", "Doe"), "John Doe"));
    }

    /** Tests that name matching works on just the given name. */
    public void testMatches_exactMatchOnGivenName() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", "Doe"), "John"));
    }

    /** Tests that name matching works on just the family name. */
    public void testMatches_exactMatchOnFamilyName() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", "Doe"), "Doe"));
    }

    /** Tests that name matching works on just the prefix of a given name. */
    public void testMatches_prefixMatchOnGivenName() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", "Doe"), "Jo"));
    }

    /** Tests that name watching works on just the prefix of a family name. */
    public void testMatches_prefixMatchOnFamilyName() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", "Doe"), "Do"));
    }

    /** Tests that prefix matching works when multiple parts of the name are given. */
    public void testMatches_prefixMatchOnGivenAndFamilyName() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", "Doe"), "John D"));
    }

    /** Tests that prefix matching on given names works when the given name has multiple words. */
    public void testMatches_prefixMatchOnMultiwordGivenName() {
        assertTrue(mNameFilter.matches(getPatientWithName("Anna Marie", "Smith"), "Anna Ma"));
    }

    /** Tests that prefix matching on family names works when the family name has multiple words. */
    public void testMatches_prefixMatchOnMultiwordFamilyName() {
        assertTrue(mNameFilter.matches(getPatientWithName("Dick", "Van Dyke"), "Van Dy"));
    }

    /** Tests that name matching is case-insensitive. */
    public void testMatches_isCaseInsensitive() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", "Doe"), "JOHN"));
    }

    /** Tests that name matching works on unicode names. */
    public void testMatches_supportsUnicode() {
        assertTrue(mNameFilter.matches(getPatientWithName("சுப்ரமணிய", "பாரதியார்"), "பாரதியார்"));
    }

    /** Tests that a non-matching search query does not match. */
    public void testMatches_negativeMatch() {
        assertFalse(mNameFilter.matches(getPatientWithName("John", "Doe"), "Jim"));
    }

    /** Tests that a query with a dash matches a patient with an unknown family name. */
    public void testMatches_dashMatchesUnknownFamilyName() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", null), "-"));
    }

    /** Tests that a query with a dash matches a patient with an unknown given name. */
    public void testMatches_dashMatchesUnknownGivenName() {
        assertTrue(mNameFilter.matches(getPatientWithName(null, "Doe"), "-"));
    }

    /** Tests that a query with a dash matches a patient with unknown family AND given names. */
    public void testMatches_dashMatchesCompletelyUnknownName() {
        assertTrue(mNameFilter.matches(getPatientWithName(null, null), "-"));
    }

    /**
     * Tests that a query with multiple dashes matches a patient with unknown family AND given
     * names.
     */
    public void testMatches_doubleDashMatchesCompletelyUnknownName() {
        assertTrue(mNameFilter.matches(getPatientWithName(null, null), "- -"));
    }

    /** Tests that a Unicode dash still matches an unknown name. */
    public void testMatches_unicodeDashMatchesUnknownName() {
        assertTrue(mNameFilter.matches(getPatientWithName(null, "Doe"), "⸗"));
    }

    /**
     * Tests that a query with both a dash and a family name matches a patient with an unknown given
     * name and matching family name.
     */
    public void testMatches_unknownGivenNameWithMatchingFamilyName() {
        assertTrue(mNameFilter.matches(getPatientWithName(null, "Doe"), "- Doe"));
    }

    /**
     * Tests that a query with both a dash and a given name matches a patient with an unknown family
     * name and matching given name.
     */
    public void testMatches_unknownFamilyNameWithMatchingGivenName() {
        assertTrue(mNameFilter.matches(getPatientWithName("John", null), "John -"));
    }

    /** Tests that a dash does not match a patient with a fully-known name. */
    public void testMatches_dashDoesNotMatchKnownName() {
        assertFalse(mNameFilter.matches(getPatientWithName("John", "Doe"), "-"));
    }

    private AppPatient getPatientWithName(String givenName, String familyName) {
        return AppPatient.builder()
                .setGivenName(Utils.nameOrUnknown(givenName))
                .setFamilyName(Utils.nameOrUnknown(familyName))
                .build();
    }
}