#!/usr/bin/ruby

LATS = [40.3225, 41.0721]
LONS = [-74.8375, -73.1209]

def valid_coord(coords)
  def between(bounds, x)
    x > bounds.min && x < bounds.max
  end

  between(LONS, coords[0]) &&
  between(LATS, coords[1]) &&
  between(LONS, coords[2]) &&
  between(LATS, coords[3])
end

while line = gets
  coords = line.split(',').map(&:to_f)
  puts coords.join(',') if valid_coord(coords)
end
