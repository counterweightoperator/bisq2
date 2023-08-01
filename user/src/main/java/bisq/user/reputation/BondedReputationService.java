/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.user.reputation;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.data.ByteArray;
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Getter
@Slf4j
public class BondedReputationService extends SourceReputationService<AuthorizedBondedReputationData> {
    public static final long WEIGHT = 100;
    public static final double MAX_AGE = 100;
    public static final long MAX_DAYS_AGE_SCORE = 365;

    public BondedReputationService(NetworkService networkService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService,
                                   BannedUserService bannedUserService,
                                   AuthorizedBondedRolesService authorizedBondedRolesService) {
        super(networkService, userIdentityService, userProfileService, bannedUserService, authorizedBondedRolesService);
    }

    @Override
    protected Optional<AuthorizedBondedReputationData> findRelevantData(AuthorizedDistributedData authorizedDistributedData) {
        return authorizedDistributedData instanceof AuthorizedBondedReputationData ?
                Optional.of((AuthorizedBondedReputationData) authorizedDistributedData) :
                Optional.empty();
    }

    @Override
    protected ByteArray getDataKey(AuthorizedBondedReputationData data) {
        return new ByteArray(data.getHash());
    }

    @Override
    protected ByteArray getUserProfileKey(UserProfile userProfile) {
        return userProfile.getBondedReputationKey();
    }

    @Override
    public long calculateScore(AuthorizedBondedReputationData data) {
        return doCalculateScore(data.getAmount(), data.getLockTime(), getAgeInDays(data.getTime()));
    }

    public static long doCalculateScore(long amount, long lockTime, long ageInDays) {
        long score = calculateScore(amount, ageInDays, lockTime);
        long ageScore = calculateAgeScore(amount, ageInDays);
        return score + ageScore;
    }

    private static long calculateScore(long amount, long ageInDays, long lockTime) {
        double decayFactor = Math.max(0, MAX_AGE - ageInDays) / MAX_AGE;
        return MathUtils.roundDoubleToLong(amount / 100d * lockTime / 10000d * decayFactor * WEIGHT);
    }

    private static long calculateAgeScore(long score, long ageInDays) {
        long boundedAgeInDays = Math.min(MAX_DAYS_AGE_SCORE, ageInDays);
        return MathUtils.roundDoubleToLong(score * boundedAgeInDays / 100d);
    }
    
  /*  @Override
    public long calculateScore(AuthorizedBondedReputationData data) {
        long score = calculateScore(data.getAmount(), data.getLockTime());
        long ageScore = calculateAgeScore(score, data.getTime());
        return score * WEIGHT + ageScore * AGE_WEIGHT;
    }
    
    private static long calculateScore(long amount, long lockTime) {
        return Math.round(amount / 100d * lockTime / 10000d);
    }

    private static long calculateAgeScore(long score, long time) {
        return score * getAgeInDays(time);
    }
    */


}