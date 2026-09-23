import { Observable } from 'rxjs';
import { ProfilePage, ProfileSearchCriteria } from '../models/profile-administration.model';

/** Puerto de consulta read-only de perfiles administrativos. */
export abstract class ProfileRepository {
  abstract search(criteria: ProfileSearchCriteria): Observable<ProfilePage>;
}
