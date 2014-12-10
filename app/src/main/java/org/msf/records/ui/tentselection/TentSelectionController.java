package org.msf.records.ui.tentselection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.msf.records.R;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Zone;
import org.msf.records.utils.EventBusRegistrationInterface;

import android.os.SystemClock;
import android.util.Log;

/**
 * Controller for {@link TentSelectionActivity}.
 *
 * Avoid adding untestable dependencies to this class.
 */
final class TentSelectionController {

	private static final String TAG = TentSelectionController.class.getSimpleName();
	private static final boolean DEBUG = true;

	public interface Ui {
		void switchToTentSelectionScreen();
		void switchToPatientListScreen();
		void launchActivityForLocation(LocationSubtree location);
		void showErrorMessage(int stringResourceId);
	}

	public interface TentFragmentUi {
		void setTents(List<LocationSubtree> tents);
		void setPatientCount(int patientCount);
		void setTriagePatientCount(int patientCount);
		void setDischargedPatientCount(int dischargedPatientCount);
		void showSpinner(boolean show);
	}

	private final LocationManager mLocationManager;
	private final Ui mUi;
	private final Set<TentFragmentUi> mFragmentUis = new HashSet<>();
	private final EventBusRegistrationInterface mEventBus;
	private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();

	private boolean mLoadedLocationTree;
	private long mLoadRequestTimeMs;
	@Nullable private LocationTree mLocationTree;
	@Nullable private LocationSubtree mTriageZone;
	@Nullable private LocationSubtree mDischargedZone;

	public TentSelectionController(
			LocationManager locationManager,
			Ui ui,
			EventBusRegistrationInterface eventBus) {
		mLocationManager = locationManager;
		mUi = ui;
		mEventBus = eventBus;
	}

	public void init() {
		mEventBus.register(mEventBusSubscriber);
		if (DEBUG) {
			Log.d(TAG, "Controller inited. Loaded tree: " + mLoadedLocationTree + ". Tree: " + mLocationTree);
		}
		if (!mLoadedLocationTree) {
			mLoadRequestTimeMs = SystemClock.elapsedRealtime();
			mLocationManager.loadLocations();
		}
		for (TentFragmentUi fragmentUi : mFragmentUis) {
			populateFragmentUi(fragmentUi);
		}
	}

	public void attachFragmentUi(TentFragmentUi fragmentUi) {
		if (DEBUG) {
			Log.d(TAG, "Attached new fragment UI: " + fragmentUi);
		}
		mFragmentUis.add(fragmentUi);
		populateFragmentUi(fragmentUi);
	}

	public void detachFragmentUi(TentFragmentUi fragmentUi) {
		if (DEBUG) {
			Log.d(TAG, "Detached fragment UI: " + fragmentUi);
		}
		mFragmentUis.remove(fragmentUi);
	}

	/** Frees any resources used by the controller. */
	public void suspend() {
		if (DEBUG) {
			Log.d(TAG, "Controller suspended.");
		}
		mEventBus.unregister(mEventBusSubscriber);
	}

	/** Call when the user presses the search button. */
	public void onSearchPressed() {
		mUi.switchToPatientListScreen();
	}

	/** Call when the user exits search mode. */
	public void onSearchCancelled() {
		mUi.switchToTentSelectionScreen();
	}

	/** Call when the user presses the discharged zone. */
	public void onDischargedPressed() {
		mUi.launchActivityForLocation(mDischargedZone);
	}

	/** Call when the user presses the triage zone. */
	public void onTriagePressed() {
		mUi.launchActivityForLocation(mTriageZone);
	}

	/** Call when the user presses a tent. */
	public void onTentSelected(LocationSubtree tent) {
		mUi.launchActivityForLocation(tent);
	}

	private void populateFragmentUi(TentFragmentUi fragmentUi) {
		fragmentUi.showSpinner(!mLoadedLocationTree);
		if (mLocationTree != null) {
			fragmentUi.setTents(mLocationTree.getTents());
	    	fragmentUi.setPatientCount(mLocationTree == null ? 0 : mLocationTree.getRoot().getPatientCount());
	        fragmentUi.setDischargedPatientCount(mDischargedZone == null ? 0 : mDischargedZone.getPatientCount());
	    	fragmentUi.setTriagePatientCount(mTriageZone == null ? 0 : mTriageZone.getPatientCount());
		}
	}


	@SuppressWarnings("unused") // Called by reflection from EventBus
	private final class EventBusSubscriber {
	    public void onEventMainThread(LocationsLoadFailedEvent event) {
	    	if (DEBUG) {
	    		Log.d(TAG, "Error loading location tree");
	    	}
	        mUi.showErrorMessage(R.string.location_load_error);
	        mLoadedLocationTree = true;
	        for (TentFragmentUi fragmentUi : mFragmentUis) {
	        	populateFragmentUi(fragmentUi);
	        }
	    }

	    public void onEventMainThread(LocationsLoadedEvent event) {
	    	if (DEBUG) {
	    		Log.d(TAG, "Loaded location tree: " + event.mLocationTree + " after "
	    				+ (SystemClock.currentThreadTimeMillis() - mLoadRequestTimeMs) + "ms");
	    	}
	    	mLocationTree = event.mLocationTree;
	        for (LocationSubtree zone : mLocationTree.getZones()) {
	            switch (zone.getLocation().uuid) {
	                case Zone.TRIAGE_ZONE_UUID:
	                    mTriageZone = zone;
	                    break;
	                // TODO(akalachman): Revisit if discharged should be treated differently.
	                case Zone.DISCHARGED_ZONE_UUID:
	                    mDischargedZone = zone;
	                    break;
	            }
	        }
	        mLoadedLocationTree = true;
	        for (TentFragmentUi fragmentUi : mFragmentUis) {
	        	populateFragmentUi(fragmentUi);
	        }
	    }
	}
}