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

package org.projectbuendia.client.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.json.JsonObservation;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Observations;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An encounter in the app model. Encounters contain one or more observations taken at a particular
 * timestamp. For more information on encounters and observations, see the official OpenMRS
 * documentation here:
 * <a href="https://wiki.openmrs.org/display/docs/Encounters+and+observations">
 * https://wiki.openmrs.org/display/docs/Encounters+and+observations"
 * </a>
 */
@Immutable
public class Encounter extends Base<String> {
    public final String patientUuid;
    public final
    @Nullable String encounterUuid;
    public final DateTime timestamp;
    public final Observation[] observations;
    public final String[] orderUuids;
    public final @Nullable String userUuid;

    /**
     * Creates a new Encounter for the given patient.
     * @param patientUuid   The UUID of the patient.
     * @param encounterUuid The UUID of this encounter, or null for encounters created on the client.
     * @param timestamp     The encounter time.
     * @param observations  An array of observations to include in the encounter.
     * @param orderUuids    A list of UUIDs of the orders executed during this encounter.
     */
    public Encounter(
        String patientUuid,
        @Nullable String encounterUuid,
        DateTime timestamp,
        Observation[] observations,
        String[] orderUuids,
        @Nullable String userUuid) {
        id = encounterUuid;
        this.patientUuid = patientUuid;
        this.encounterUuid = id;
        this.timestamp = timestamp;
        this.observations = observations == null ? new Observation[] {} : observations;
        this.orderUuids = orderUuids == null ? new String[] {} : orderUuids;
        this.userUuid = userUuid;
    }

    /**
     * Creates an instance of {@link Encounter} from a network
     * {@link JsonEncounter} object and corresponding patient UUID.
     */
    // TODO: JsonEncounter includes a patient_uuid field, use that instead of passing it separately.
    public static Encounter fromJson(String patientUuid, JsonEncounter encounter) {
        List<Observation> observations = new ArrayList<>();
        if (encounter.observations != null) {
            for (JsonObservation observation : encounter.observations) {
                observations.add(new Observation(
                    observation.concept_uuid,
                    observation.value
                ));
            }
        }
        return new Encounter(patientUuid, encounter.uuid, encounter.timestamp,
            observations.toArray(new Observation[observations.size()]), encounter.order_uuids, null);
    }

    /** Serializes this into a {@link JSONObject}. */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(Server.PATIENT_UUID_KEY, patientUuid);
        json.put(Server.ENCOUNTER_TIMESTAMP, timestamp.getMillis()/1000);
        if (observations.length > 0) {
            JSONArray observationsJson = new JSONArray();
            for (Observation obs : observations) {

                observationsJson.put(obs.toJson());
            }
            json.put(Server.ENCOUNTER_OBSERVATIONS_KEY, observationsJson);
        }
        if (orderUuids.length > 0) {
            JSONArray orderUuidsJson = new JSONArray();
            for (String orderUuid : orderUuids) {
                orderUuidsJson.put(orderUuid);
            }
            json.put(Server.ENCOUNTER_ORDER_UUIDS, orderUuidsJson);
        }
        json.put(Server.ENCOUNTER_USER_UUID, userUuid);
        return json;
    }

    /**
     * Converts this instance of {@link Encounter} to an array of
     * {@link android.content.ContentValues} objects for insertion into a database or content
     * provider.
     */
    public ContentValues[] toContentValuesArray() {
        ContentValues[] cvs = new ContentValues[observations.length + orderUuids.length];
        for (int i = 0; i < observations.length; i++) {
            Observation obs = observations[i];
            ContentValues cv = new ContentValues();
            cv.put(Observations.CONCEPT_UUID, obs.conceptUuid);
            cv.put(Observations.ENCOUNTER_MILLIS, timestamp.getMillis());
            cv.put(Observations.ENCOUNTER_UUID, encounterUuid);
            cv.put(Observations.PATIENT_UUID, patientUuid);
            cv.put(Observations.VALUE, obs.value);
            cvs[i] = cv;
        }
        for (int i = 0; i < orderUuids.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(Observations.CONCEPT_UUID, AppModel.ORDER_EXECUTED_CONCEPT_UUID);
            cv.put(Observations.ENCOUNTER_MILLIS, timestamp.getMillis());
            cv.put(Observations.ENCOUNTER_UUID, encounterUuid);
            cv.put(Observations.PATIENT_UUID, patientUuid);
            cv.put(Observations.VALUE, orderUuids[i]);
            cvs[observations.length + i] = cv;
        }
        return cvs;
    }

    /** Represents a single observation within this encounter. */
    public static final class Observation {
        public final String conceptUuid;
        public final String value;

        public Observation(String conceptUuid, String value) {
            this.conceptUuid = conceptUuid;
            this.value = value;
        }

        public JSONObject toJson() {
            JSONObject observationJson = new JSONObject();
            try {
                observationJson.put(Server.OBSERVATION_QUESTION_UUID, conceptUuid);
                observationJson.put(Server.OBSERVATION_ANSWER, value);
            } catch (JSONException jsonException) {
                // Should never occur, JSONException is only thrown for a null key or an invalid
                // numeric value, neither of which will occur in this API.
                throw new RuntimeException(jsonException);
            }
            return observationJson;
        }
    }

    /**
     * An {@link CursorLoader} that loads {@link Encounter}s. Expects the {@link Cursor} to
     * contain only a single encounter, represented by multiple observations, with one observation per
     * row.
     * <p/>
     * <p>Unlike other {@link CursorLoader}s, {@link Encounter.Loader} must be instantiated
     * once per patient, since {@link Encounter} contains the patient's UUID as one of its fields,
     * which is not present in the database representation of an encounter.
     */
    public static class Loader implements CursorLoader<Encounter> {
        private String mPatientUuid;

        public Loader(String patientUuid) {
            mPatientUuid = patientUuid;
        }

        @Override public Encounter fromCursor(Cursor cursor) {
            final String encounterUuid = cursor.getString(
                cursor.getColumnIndex(Observations.ENCOUNTER_UUID));
            final long millis = cursor.getLong(
                cursor.getColumnIndex(Observations.ENCOUNTER_MILLIS));
            List<Observation> observations = new ArrayList<>();
            cursor.move(-1);
            while (cursor.moveToNext()) {
                String value = cursor.getString(cursor.getColumnIndex(Observations.VALUE));
                observations.add(new Observation(
                    cursor.getString(cursor.getColumnIndex(Observations.CONCEPT_UUID)),
                    value
                ));
            }
            return new Encounter(mPatientUuid, encounterUuid, new DateTime(millis),
                observations.toArray(new Observation[observations.size()]), null, null);
        }
    }
}
