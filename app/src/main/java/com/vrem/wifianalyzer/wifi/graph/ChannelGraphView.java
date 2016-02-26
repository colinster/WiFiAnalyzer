/*
 *    Copyright (C) 2015 - 2016 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.vrem.wifianalyzer.wifi.graph;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.vrem.wifianalyzer.R;
import com.vrem.wifianalyzer.wifi.model.SortBy;
import com.vrem.wifianalyzer.wifi.model.WiFiBand;
import com.vrem.wifianalyzer.wifi.model.WiFiData;
import com.vrem.wifianalyzer.wifi.model.WiFiDetails;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

class ChannelGraphView {
    private final WiFiBand wiFiBand;
    private final GraphView graphView;
    private final Map<String, LineGraphSeries<DataPoint>> seriesMap;
    private final GraphViewUtils graphViewUtils;
    private boolean defaultAdded;

    private ChannelGraphView(@NonNull GraphViewBuilder graphViewBuilder, @NonNull Resources resources, @NonNull WiFiBand wiFiBand, boolean scrollable, boolean evenOnly) {
        this.wiFiBand = wiFiBand;
        this.graphView = makeGraphView(graphViewBuilder, resources, scrollable, evenOnly);
        this.seriesMap = new TreeMap<>();
        this.graphViewUtils = new GraphViewUtils(graphView, seriesMap);
        this.defaultAdded = false;
    }

    static ChannelGraphView make2(@NonNull GraphViewBuilder graphViewBuilder, @NonNull Resources resources) {
        return new ChannelGraphView(graphViewBuilder, resources, WiFiBand.TWO, false, false);
    }

    static ChannelGraphView make5(@NonNull GraphViewBuilder graphViewBuilder, @NonNull Resources resources) {
        return new ChannelGraphView(graphViewBuilder, resources, WiFiBand.FIVE, true, true);
    }

    private GraphView makeGraphView(@NonNull GraphViewBuilder graphViewBuilder, @NonNull Resources resources, boolean scrollable, boolean evenOnly) {
        int minX = wiFiBand.getChannelFirst() - WiFiBand.CHANNEL_SPREAD;
        int maxX = minX + GraphViewBuilder.CNT_X - 1;

        return graphViewBuilder
                .setLabelFormatter(new AxisLabel(wiFiBand.getChannelFirst(), wiFiBand.getChannelLast()).setEvenOnly(evenOnly))
                .setVerticalTitle(resources.getString(R.string.graph_axis_y))
                .setHorizontalTitle(resources.getString(R.string.graph_channel_axis_x))
                .setScrollable(scrollable)
                .setMinX(minX)
                .setMaxX(maxX)
                .build();
    }

    void update(@NonNull WiFiData wiFiData) {
        Set<String> newSeries = new TreeSet<>();
        addConnection(wiFiData, newSeries);
        addDefaultsSeries();
        addWiFiDetails(wiFiData, newSeries);
        graphViewUtils.updateSeries(newSeries);
        graphViewUtils.updateLegend();
        graphViewUtils.setVisibility(wiFiBand);
    }

    private void addConnection(@NonNull WiFiData wiFiData, Set<String> newSeries) {
        WiFiDetails connection = wiFiData.getConnection();
        if (connection != null && wiFiBand.equals(connection.getWiFiBand())) {
            addData(newSeries, connection);
        }
    }

    private void addWiFiDetails(@NonNull WiFiData wiFiData, Set<String> newSeries) {
        for (WiFiDetails wiFiDetails : wiFiData.getWiFiList(wiFiBand, SortBy.CHANNEL)) {
            if (wiFiDetails.isConnected()) {
                continue;
            }
            addData(newSeries, wiFiDetails);
        }
    }

    private void addData(@NonNull Set<String> newSeries, @NonNull WiFiDetails wiFiDetails) {
        String key = wiFiDetails.getTitle();
        newSeries.add(key);
        LineGraphSeries<DataPoint> series = seriesMap.get(key);
        if (series == null) {
            series = new LineGraphSeries<>(createDataPoints(wiFiDetails));
            setSeriesOptions(series, wiFiDetails);
            graphView.addSeries(series);
            seriesMap.put(key, series);
        } else {
            series.resetData(createDataPoints(wiFiDetails));
        }
    }

    private void setSeriesOptions(@NonNull LineGraphSeries<DataPoint> series, @NonNull WiFiDetails wiFiDetails) {
        if (wiFiDetails.isConnected()) {
            series.setColor(GraphColor.BLUE.getPrimary());
            series.setBackgroundColor(GraphColor.BLUE.getBackground());
            series.setThickness(6);
        } else {
            GraphColor graphColor = GraphColor.findColor();
            series.setColor(graphColor.getPrimary());
            series.setBackgroundColor(graphColor.getBackground());
            series.setThickness(2);
        }
        series.setDrawBackground(true);
        series.setTitle(wiFiDetails.getTitle() + " " + wiFiDetails.getChannel());
    }

    private DataPoint[] createDataPoints(@NonNull WiFiDetails wiFiDetails) {
        int channel = wiFiDetails.getChannel();
        int level = wiFiDetails.getLevel();
        return new DataPoint[]{
                new DataPoint(channel - WiFiBand.CHANNEL_SPREAD, GraphViewBuilder.MIN_Y),
                new DataPoint(channel - WiFiBand.CHANNEL_SPREAD / 2, level),
                new DataPoint(channel, level),
                new DataPoint(channel + WiFiBand.CHANNEL_SPREAD / 2, level),
                new DataPoint(channel + WiFiBand.CHANNEL_SPREAD, GraphViewBuilder.MIN_Y)
        };
    }

    private void addDefaultsSeries() {
        if (defaultAdded) {
            return;
        }

        int minValue = wiFiBand.getChannelFirst() - WiFiBand.CHANNEL_SPREAD;
        int maxValue = wiFiBand.getChannelLast() + WiFiBand.CHANNEL_SPREAD;
        if (maxValue % 2 != 0) {
            maxValue++;
        }
        DataPoint[] dataPoints = new DataPoint[]{
                new DataPoint(minValue, GraphViewBuilder.MIN_Y),
                new DataPoint(maxValue, GraphViewBuilder.MIN_Y)
        };

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        series.setColor(GraphColor.TRANSPARENT.getPrimary());
        series.setDrawBackground(false);
        series.setThickness(0);
        series.setTitle("");
        graphView.addSeries(series);
        defaultAdded = true;
    }
}