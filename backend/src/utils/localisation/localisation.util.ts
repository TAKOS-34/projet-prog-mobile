import { BadRequestException, Injectable } from '@nestjs/common';
import NodeGeocoder from 'node-geocoder';

@Injectable()
export class LocalisationUtil {
    private readonly geocoder = NodeGeocoder({ provider: 'openstreetmap' });

    async getCoordinates(address: string): Promise<{ long: number, lat: number }> {
        const res = await this.geocoder.geocode(address);
        if (!res.length || !res[0].latitude || !res[0].longitude) {
            throw new BadRequestException('Localisation is not correct');
        }
        return { long: res[0].longitude, lat: res[0].latitude };
    }
}