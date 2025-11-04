package sh.fyz.fiber.core.challenge.internal;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.params.PathVariable;
import sh.fyz.fiber.annotations.params.RequestBody;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.challenge.Challenge;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;
import sh.fyz.fiber.core.security.annotations.AuditLog;

import java.util.Map;
import java.util.Optional;

@Controller("/internal/challenge")
public class ChallengeController {
    public ChallengeController() {
    }

    @RequestMapping(value = "/verify/{challengeID}", method = RequestMapping.Method.POST)
    @AuditLog(action = "CHALLENGE_VERIFICATION", logParameters = true, maskSensitiveData = true)
    public ResponseEntity<Object> verifyChallenge(@PathVariable("challengeID") String challengeID, @RequestBody Map<String, String> response,
                                                  HttpServletRequest request, HttpServletResponse httpResponse) {
        ChallengeRegistry challengeRegistry = FiberServer.get().getChallengeRegistry();
        Optional<Challenge> challenge = challengeRegistry.getChallenge(challengeID);
        if (challenge.isEmpty()) {
            return ResponseEntity.notFound("Challenge not found");
        }
        ResponseEntity<Object> entity = challengeRegistry.validateChallenge(challengeID, response, request, httpResponse);
        if(entity != null) {
            return entity;
        }
        return ResponseEntity.gone("Challenge expired");
    }

}
