package app.carpecoin.priceGraph.models

import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

data class PriceGraphData(var asks:LineGraphSeries<DataPoint>?,
                          var bids: LineGraphSeries<DataPoint>?)