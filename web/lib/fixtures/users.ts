import { portraitUrlFor } from "../utils/avatar";
import type { User } from "./types";

export const CURRENT_USER_ID = "u-you-0001";

function withPortrait<T extends { userId: string; profilePictureUrl: string | null }>(u: T): T {
  return { ...u, profilePictureUrl: u.profilePictureUrl ?? portraitUrlFor(u.userId) };
}

export const currentUser: User = withPortrait({
  userId: CURRENT_USER_ID,
  email: "you@example.com",
  firstName: "Alex",
  lastName: "Chen",
  academicTitle: "PhD Candidate",
  institution: "ETH Zurich",
  profilePictureUrl: null
});

const rawOthers: User[] = [
  {
    userId: "u-0002",
    email: "alice.smith@mit.edu",
    firstName: "Alice",
    lastName: "Smith",
    academicTitle: "Prof.",
    institution: "MIT",
    profilePictureUrl: null
  },
  {
    userId: "u-0003",
    email: "somchai.r@chula.ac.th",
    firstName: "Somchai",
    lastName: "Ratanakul",
    academicTitle: "Assoc. Prof.",
    institution: "Chulalongkorn University",
    profilePictureUrl: null
  },
  {
    userId: "u-0004",
    email: "h.mueller@tum.de",
    firstName: "Hannah",
    lastName: "Müller",
    academicTitle: "Dr.",
    institution: "TU Munich",
    profilePictureUrl: null
  },
  {
    userId: "u-0005",
    email: "yuki.tanaka@u-tokyo.ac.jp",
    firstName: "Yuki",
    lastName: "Tanaka",
    academicTitle: "PhD Candidate",
    institution: "University of Tokyo",
    profilePictureUrl: null
  },
  {
    userId: "u-0006",
    email: "priya.sharma@iitb.ac.in",
    firstName: "Priya",
    lastName: "Sharma",
    academicTitle: "Prof.",
    institution: "IIT Bombay",
    profilePictureUrl: null
  },
  {
    userId: "u-0007",
    email: "marco.rossi@polimi.it",
    firstName: "Marco",
    lastName: "Rossi",
    academicTitle: "Dr.",
    institution: "Politecnico di Milano",
    profilePictureUrl: null
  },
  {
    userId: "u-0008",
    email: "aisha.okonkwo@uct.ac.za",
    firstName: "Aisha",
    lastName: "Okonkwo",
    academicTitle: "Prof.",
    institution: "University of Cape Town",
    profilePictureUrl: null
  },
  {
    userId: "u-0009",
    email: "jean.dupont@inria.fr",
    firstName: "Jean",
    lastName: "Dupont",
    academicTitle: "Research Scientist",
    institution: "INRIA Paris",
    profilePictureUrl: null
  },
  {
    userId: "u-0010",
    email: "lina.kowalski@ethz.ch",
    firstName: "Lina",
    lastName: "Kowalski",
    academicTitle: "Postdoc",
    institution: "ETH Zurich",
    profilePictureUrl: null
  },
  {
    userId: "u-0011",
    email: "diego.fernandez@unam.mx",
    firstName: "Diego",
    lastName: "Fernández",
    academicTitle: "PhD Candidate",
    institution: "UNAM",
    profilePictureUrl: null
  },
  {
    userId: "u-0012",
    email: "emma.ohara@tcd.ie",
    firstName: "Emma",
    lastName: "O'Hara",
    academicTitle: "Dr.",
    institution: "Trinity College Dublin",
    profilePictureUrl: null
  },
  {
    userId: "u-0013",
    email: "rajesh.iyer@stanford.edu",
    firstName: "Rajesh",
    lastName: "Iyer",
    academicTitle: "Prof.",
    institution: "Stanford University",
    profilePictureUrl: null
  },
  {
    userId: "u-0014",
    email: "nina.petrov@bsu.by",
    firstName: "Nina",
    lastName: "Petrov",
    academicTitle: "Assoc. Prof.",
    institution: "Belarusian State University",
    profilePictureUrl: null
  },
  {
    userId: "u-0015",
    email: "kenji.ito@kyoto-u.ac.jp",
    firstName: "Kenji",
    lastName: "Ito",
    academicTitle: "Dr.",
    institution: "Kyoto University",
    profilePictureUrl: null
  },
  {
    userId: "u-0016",
    email: "fatima.alghamdi@kaust.edu.sa",
    firstName: "Fatima",
    lastName: "Al-Ghamdi",
    academicTitle: "PhD Candidate",
    institution: "KAUST",
    profilePictureUrl: null
  },
  {
    userId: "u-0017",
    email: "lukas.svensson@kth.se",
    firstName: "Lukas",
    lastName: "Svensson",
    academicTitle: "Prof.",
    institution: "KTH Stockholm",
    profilePictureUrl: null
  },
  {
    userId: "u-0018",
    email: "ana.ferreira@usp.br",
    firstName: "Ana",
    lastName: "Ferreira",
    academicTitle: "Research Scientist",
    institution: "University of São Paulo",
    profilePictureUrl: null
  },
  {
    userId: "u-0019",
    email: "michael.oconnor@unimelb.edu.au",
    firstName: "Michael",
    lastName: "O'Connor",
    academicTitle: "Dr.",
    institution: "University of Melbourne",
    profilePictureUrl: null
  },
  {
    userId: "u-0020",
    email: "chen.wei@pku.edu.cn",
    firstName: "Wei",
    lastName: "Chen",
    academicTitle: "Prof.",
    institution: "Peking University",
    profilePictureUrl: null
  }
];

export const otherUsers: User[] = rawOthers.map(withPortrait);

export const allUsers: User[] = [currentUser, ...otherUsers];

export function findUser(userId: string): User | undefined {
  return allUsers.find((u) => u.userId === userId);
}
