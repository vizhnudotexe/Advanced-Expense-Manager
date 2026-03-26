const db = require('./db');

async function inspect() {
  // Just check table names
  const tables = await db.query(
    "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name"
  );
  console.log('=== TABLES IN DATABASE ===');
  tables.rows.forEach(r => console.log(r.table_name));

  // Check columns per table
  for (const t of tables.rows) {
    const cols = await db.query(
      "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema='public' AND table_name=$1 ORDER BY ordinal_position",
      [t.table_name]
    );
    console.log('\n--- ' + t.table_name + ' ---');
    cols.rows.forEach(c => console.log('  ' + c.column_name + ' : ' + c.data_type));
  }
  process.exit(0);
}

inspect().catch(err => { console.error(err); process.exit(1); });
