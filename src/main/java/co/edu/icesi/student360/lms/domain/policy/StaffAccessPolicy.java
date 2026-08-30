package co.edu.icesi.student360.lms.domain.policy;

import co.edu.icesi.student360.common.api.exception.AccessDeniedForSubjectException;
import co.edu.icesi.student360.common.audit.AuthorizationBasis;
import co.edu.icesi.student360.common.audit.AuthorizationBasisHolder;
import co.edu.icesi.student360.common.authorization.StudentRecordAccessPolicy;
import co.edu.icesi.student360.common.identity.Identity;
import co.edu.icesi.student360.common.identity.IdentityContext;

/**
 * Batch reads across many students are a staff capability: a student may see themself, never a list
 * of others, so the whole request is denied for them and audited as such.
 */
public class StaffAccessPolicy {

  public void assertStaff(String subjectType, String subjectId) {
    Identity caller = IdentityContext.require();
    if (caller.hasRole(StudentRecordAccessPolicy.ADMIN)) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.ADMIN_ROLE);
      return;
    }
    if (caller.hasRole(StudentRecordAccessPolicy.ADVISOR)) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.STAFF_ROLE);
      return;
    }
    throw new AccessDeniedForSubjectException(subjectType, subjectId);
  }
}
