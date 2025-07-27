package org.traccar.geocoder;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
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
        private Long id;
        private String nameEn;
        private String nameLocal;
        private String country;
        private PreparedGeometry preparedGeometry;

        PolygonEntry(Geometry geometry,
                Long id,
                String nameEn,
                String nameLocal,
                String country) {
            this.id = id;
            this.nameEn = nameEn;
            this.nameLocal = nameLocal;
            this.country = country;
            this.preparedGeometry = PreparedGeometryFactory.prepare(geometry);
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
                        //Geometry simplifiedGeom = TopologyPreservingSimplifier.simplify(geom, 0.0001);

                        Long id = (Long) feature.getAttribute("POSITION_I"); // ID
                        String nameEn = (String) feature.getAttribute("POSITION_N"); // Eng
                        String nameLocal = (String) feature.getAttribute("POSITIO_01"); // TH COUNTRY
                        String country = (String) feature.getAttribute("COUNTRY");

                        PolygonEntry entry = new PolygonEntry(geom, id, nameEn, nameLocal, country);
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
        long startAll = System.nanoTime();

        Coordinate testCoord = new Coordinate(longitude, latitude);
        GeometryFactory geometryFactory = new GeometryFactory();
        Point testPoint = geometryFactory.createPoint(testCoord);

        long startQuery = System.nanoTime();

        // Fast spatial query using bounding box
        @SuppressWarnings("unchecked")
        List<PolygonEntry> candidates = spatialIndex.query(testPoint.getEnvelopeInternal());
        long endQuery = System.nanoTime();
        LOGGER.debug("Candidates count: {}", candidates.size());

        long startCheck = System.nanoTime();
        boolean found = false;
        String address = "No address found";
        for (PolygonEntry entry : candidates) {
            if (entry.preparedGeometry.contains(testPoint)) {
                LOGGER.debug("Point ({}, {}) is inside polygon with ID: {}", longitude, latitude, entry.id);
                address = String.format(
                        """
                {"placeId":%s,"nameEnglish":"%s","nameLocale":"%s","countryCode":"%s"}""",
                        entry.id,
                        entry.nameEn,
                        entry.nameLocal,
                        entry.country.toUpperCase());
                LOGGER.debug("Address found:{}", address);
                found = true;
                break;
            }
        }
        long endCheck = System.nanoTime();
        LOGGER.debug("Time - total: {}ms, spatial query: {}ms, contains check: {}ms",
        (endCheck - startAll) / 1_000_000,
        (endQuery - startQuery) / 1_000_000,
        (endCheck - startCheck) / 1_000_000);

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
