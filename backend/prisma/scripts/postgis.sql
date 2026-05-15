CREATE EXTENSION IF NOT EXISTS postgis;

CREATE INDEX IF NOT EXISTS localisation_spatial_idx ON "Localisation" USING GIST ((ST_MakePoint(long, lat)::geography));