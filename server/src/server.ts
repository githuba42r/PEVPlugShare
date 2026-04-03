import { createApp } from './app';
import { config } from './config';

const app = createApp();

app.listen(config.port, () => {
  console.log(`PEVPlugShare server listening on port ${config.port}`);
  console.log(`Database backend: ${config.dbBackend}`);
  console.log(`Log level: ${config.logLevel}`);
});
