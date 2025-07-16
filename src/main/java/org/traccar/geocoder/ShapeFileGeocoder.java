package org.traccar.geocoder;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.StatisticsManager;
import org.geotools.api.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShapeFileGeocoder implements Geocoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeFileGeocoder.class);

    private STRtree spatialIndex;

    static class PolygonEntry {
        Geometry geometry;
        Long id;
        String name_en;
        String name_local;
        String country;

        PolygonEntry(Geometry geometry,
                Long id,
                String name_en,
                String name_local,
                String country) {
            this.geometry = geometry;
            this.id = id;
            this.name_en = name_en;
            this.name_local = name_local;
            this.country = country;
        }
    }

    public ShapeFileGeocoder(Config config) {
        // Initialize the spatial index and load polygons from shapefile
        try {
            String shapefilePath = config.getString(Keys.SHAPEFILE_PATH);
            LOGGER.info("Loading shapefile from path: {}", shapefilePath);
            // Load shapefile
            File file = new File(shapefilePath);
            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();

            // Build STRtree (R-tree spatial index)
            spatialIndex = new STRtree();
            List<PolygonEntry> allPolygons = new ArrayList<>();

            try (SimpleFeatureIterator features = featureSource.getFeatures().features()) {
                while (features.hasNext()) {
                    try {
                        SimpleFeature feature = features.next();
                        Geometry geom = (Geometry) feature.getDefaultGeometry();
                        if (geom == null || geom.isEmpty()) {
                            continue; // Skip empty geometries
                        }

                        Long id = (Long) feature.getAttribute("POSITION_I"); // ID
                        String name_en = (String) feature.getAttribute("POSITION_N"); // Eng
                        String name_local = (String) feature.getAttribute("POSITIO_01"); // TH COUNTRY
                        String country = (String) feature.getAttribute("COUNTRY");

                        PolygonEntry entry = new PolygonEntry(geom, id, name_en, name_local, country);
                        allPolygons.add(entry);
                        spatialIndex.insert(geom.getEnvelopeInternal(), entry);
                    } catch (Exception e) {
                        LOGGER.warn("Error processing feature: {}", e.getMessage());
                    }
                }
            }

            spatialIndex.build();
            LOGGER.info("Shapefile loaded successfully with {} polygons.", allPolygons.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAddress(double latitude, double longitude, ReverseGeocoderCallback callback) {
        LOGGER.debug("Finding address for coordinates: {}, {}", latitude, longitude);
        Coordinate testCoord = new Coordinate(longitude, latitude);
        GeometryFactory geometryFactory = new GeometryFactory();
        Point testPoint = geometryFactory.createPoint(testCoord);

        // Fast spatial query using bounding box
        @SuppressWarnings("unchecked")
        List<PolygonEntry> candidates = spatialIndex.query(testPoint.getEnvelopeInternal());

        boolean found = false;
        String address = "No address found";
        for (PolygonEntry entry : candidates) {
            if (entry.geometry.contains(testPoint)) {
                LOGGER.debug("Point ({}, {}) is inside polygon with ID: {}", longitude, latitude, entry.id);
                address = String.format(
                        """
                {"placeId":%s,"nameEnglish":"%s","nameLocale":"%s","countryCode":"%s"}
                """,
                        entry.id,
                        entry.name_en,
                        entry.name_local,
                        entry.country.toUpperCase()).replaceAll("\n", "");
                LOGGER.debug("Address found:{}", address);
                found = true;
                break;
            }
        }

        if (!found) {
            LOGGER.info("Point ({}, {}) is NOT inside any polygon.", longitude, latitude);
        }
        if (callback != null) {
            callback.onSuccess(address);
            return null;
        }
        return address;
    }

    @Override
    public void setStatisticsManager(StatisticsManager statisticsManager) {
    }

}
