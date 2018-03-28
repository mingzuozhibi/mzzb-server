DELETE r
FROM record r
  LEFT JOIN disc d ON r.disc_id = d.id
WHERE r.date >= d.release_date
