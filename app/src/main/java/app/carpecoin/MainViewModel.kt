package app.carpecoin

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.Transformations

class MainViewModel : ViewModel() {
    //TODO: Add logic to store 2 active exchanges in LiveData<Array>
    //TODO: Add new exchange to beginning, shift first exchange to 2nd, remove 2nd exchange
    private var toInitializeData = MutableLiveData<Boolean>()
    var text = "content here"

    companion object {
        var priceRepository = PriceRepository
    }

    val priceGraphData = Transformations.switchMap(toInitializeData) { toInitializeData ->
        priceRepository.getPricingGraphLiveData()
    }

    val isPriceGraphDataLoaded = Transformations.switchMap(toInitializeData) { toInitializeData ->
        priceRepository.getIsPriceGraphDataLoadedLiveData()
    }

    val priceGraphXAndYConstraints = Transformations.switchMap(toInitializeData) { toInitializeData ->
        priceRepository.getPricingGraphXAndYConstraintsLiveData()
    }

    fun initializeData() {
        this.toInitializeData.setValue(true)
    }

    fun setFirebasePriceGraphListeners(isLiveDataEnabled: Boolean) {
        priceRepository.startFirebaseEventListeners(isLiveDataEnabled)
    }

}