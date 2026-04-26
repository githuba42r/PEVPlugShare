import morgan from 'morgan';
import { config } from '../config';

export const requestLogger = morgan(config.logLevel === 'debug' ? 'dev' : 'combined');
