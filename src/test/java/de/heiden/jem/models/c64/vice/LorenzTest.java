package de.heiden.jem.models.c64.vice;

import de.heiden.jem.models.c64.AbstractTest;
import de.heiden.jem.models.c64.Condition;
import de.heiden.jem.models.c64.ProgramSuiteSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Testsuite 2.15.
 */
class LorenzTest extends AbstractTest {
  @ParameterizedTest(name = "{1}")
  @ProgramSuiteSource(resource = "/vice-emu-testprogs/general/Lorenz-2.15/src/start.prg", ignore = {
    "start", "nextdisk1", "nextdisk2", "finish",
    // Passing tests. Ignore them for now to get faster to the failing ones.
    "adca", "adcax", "adcay", "adcb", "adcix", "adciy", "adcz", "adczx",
    "alrb", "ancb",
    "anda", "andax", "anday", "andb", "andix", "andiy", "andz", "andzx",
    "aneb",
    "asla", "aslax", "asln", "aslz", "aslzx",
    "asoa", "asoax", "asoay", "asoix", "asoiy", "asoz", "asozx",
    "axsa", "axsix", "axsz", "axszy",
    "bccr", "bcsr", "beqr",
    "bita", "bitz",
    "bmir", "bner", "bplr", "branchwrap",
    "brkn",
    "bvcr", "bvsr",
    "clcn", "cldn", "clin", "clvn",
    "cmpa", "cmpax", "cmpay", "cmpb", "cmpix", "cmpiy", "cmpz", "cmpzx",
    "cpuport",
    "cpxa", "cpxb", "cpxz", "cpya", "cpyb", "cpyz",
    "dcma", "dcmax", "dcmay", "dcmix", "dcmiy", "dcmz", "dcmzx",
    "deca", "decax", "decz", "deczx", "dexn", "deyn",
    "eora", "eorax", "eoray", "eorb", "eorix", "eoriy", "eorz", "eorzx",
    "inca", "incax", "incz", "inczx",
    "insa", "insax", "insay", "insix", "insiy", "insz", "inszx", "inxn", "inyn",
    "jmpi", "jmpw", "jsrw",
    "lasay", "laxa", "laxay", "laxix", "laxiy", "laxz", "laxzy",
    "ldaa", "ldaax", "ldaay", "ldab", "ldaix", "ldaiy", "ldaz", "ldazx",
    "ldxa", "ldxay", "ldxb", "ldxz", "ldxzy",
    "ldya", "ldyax", "ldyb", "ldyz", "ldyzx",
    "lsea", "lseax", "lseay", "lseix", "lseiy", "lsez", "lsezx",
    "lsra", "lsrax", "lsrn", "lsrz", "lsrzx",
    "lxab",
    "mmu", "mmufetch",
    "nopa", "nopax", "nopb", "nopn", "nopz", "nopzx",
    "oraa", "oraax", "oraay", "orab", "oraix", "oraiy", "oraz", "orazx",
    "phan", "phpn", "plan", "plpn",
    "rlaa", "rlaax", "rlaay", "rlaix", "rlaiy", "rlaz", "rlazx",
    "rola", "rolax", "roln", "rolz", "rolzx",
    "rora", "rorax", "rorn", "rorz", "rorzx",
    "rraa", "rraax", "rraay", "rraix", "rraiy", "rraz", "rrazx",
    "rtin", "rtsn",
    "sbca", "sbcax", "sbcay", "sbcb-eb", "sbcb", "sbcix", "sbciy", "sbcz", "sbczx",
    "sbxb",
    "secn", "sedn", "sein",
    "shaay", "shaiy", "shsay", "shxay", "shyax",
    "staa", "staax", "staay", "staix", "staiy", "staz", "stazx",
    "stxa", "stxz", "stxzy", "stya", "styz", "styzx",
    "taxn", "tayn",
    "template",
    "trap1", "trap2", "trap3", "trap4", "trap5", "trap6", "trap7", "trap8", "trap9", "trap10",
    "trap11", "trap12", "trap13", "trap14", "trap15", "trap16", "trap17",
    "tsxn", "txan", "txsn", "tyan"
  })
  void test(Path program, String programName) throws Exception {
    loadAndRun(program);

    Condition passed = onConsole("- ok");
    Condition failed = onConsole("right", "error", " not ");
    Condition event = waitSecondsFor(999, passed, failed);
    waitCycles(1000);

    // Assert that test program exits with "OK" message.
    // Consider everything else (timeout, error messages) as a test failure.
    assertSame(passed, event);
  }
}
