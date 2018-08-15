package app.carpecoin.priceGraph.models

import androidx.lifecycle.MutableLiveData
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

data class PriceGraphLiveData(var bidsLiveData: MutableLiveData<LineGraphSeries<DataPoint>>,
                              var asksLiveData: MutableLiveData<LineGraphSeries<DataPoint>>)