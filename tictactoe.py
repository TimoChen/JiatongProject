first_player_wins = "first player wins"
second_player_wins = "second player wins"
tie = "tie"
ongoing = "ongoing"

def won_row(position,player):

	for i in range(0,2):
		if position[i][0]==player:
			if position[i][1]==player and position[i][2]==player:
				return True
			
	return False
	

def won_column(position,player):

	for i in range(0,2):
		if position[0][i]==player:
			if position[1][i]==player and position[2][i]==player:
				return True
			
	return False

def won_diagonal(position,player):

	if position[1][1]==player:
		if position[0][0]==player and position[2][2]==player:
			return True
		if position[0][2]==player and position[2][0]==player:
			return True
	return False

def detect_result(position):
	
	if won_row(position, 'X') or won_column(position,'X') or won_diagonal(position,'X'):
		return first_player_wins

	if won_row(position, 'O') or won_column(position,'O') or won_diagonal(position,'O'):
		return second_player_wins

	for i in range(0,2):
		for j in range(0,2):
			if position[i][j]!='X' and position[i][j]!='O':
				return ongoing

	return tie

def assign(position, move, player):

	rows = list(position)

	row = [list(rows[0]), list(rows[1]), list(rows[2])]

	m = list(move)

	row[m[0]][m[1]] = player
        
	return tuple([tuple(row[0]), tuple(row[1]), tuple(row[2])])

def every_move(position, step):
	print(position)

	if (detect_result(position)=="ongoing"):
		for i in range(0,3):
			for j in range(0,3):
				if (list(list(position)[i])[j]=='-'):
					if step%2==0:
						every_move(assign(position, [i,j], 'X'), step+1)
					else:
						every_move(assign(position, [i,j], 'O'), step+1)

def generateMoves(position):
	result = []

	i = 0
	for row in list(position):

		j = 0

		for cell in list(row):
			if cell=='-':
				result.append((i, j))
			j = j + 1

		i = i + 1
	
	return result

def generateNextPositions(position, playerOnMove):
	result = {}
	for move in generateMoves(position):
		result[move] = assign(position, move, playerOnMove)
	
	return result

def inputMove(position, player):

	while True:
		move = input("")
	
		rows = list(position)

		row = [list(rows[0]), list(rows[1]), list(rows[2])]

		r = int(move.split(' ')[0])
		c = int(move.split(' ')[1])

		if r>=0 and r<=2 and c>=0 and c<=2:
			if row[r][c]=='-':
				row[r][c] = player        
				return tuple([tuple(row[0]), tuple(row[1]), tuple(row[2])])
			else:
				print("Cell is occupied")
		else:
				print("Input is out of range")

def printPosition(position):
	rows = list(position)
	print(rows[0])
	print(rows[1])
	print(rows[2])

def opposite(player):
	if player=='X':
		return 'O'
	if player=='O':
		return 'X'
	return player


def getMoveValue(position, move, player, deep):	
	if deep>2:
		return 0

	current_position = assign(position, move, player)

	result = detect_result(current_position)

	if result==second_player_wins:
		return 1
	if result==first_player_wins:
		return -1
	if result==tie:
		return 0
	
	value_of_move = 10
	for move in generateNextPositions(current_position, opposite(player)):
		m = getMoveValue(current_position, move, opposite(player), deep + 1)		
		if m < value_of_move:
			value_of_move = m

	return value_of_move

current_position = (('-', '-', '-'), ('-', '-', '-'), ('-', '-', '-'))
#possible_moves = generateNextPositions(current_position, 'O')

stop = False

while not stop:
	value_of_move = -10
	best_move = None
	for move in generateNextPositions(current_position, 'O'):
		m = getMoveValue(current_position, move, 'O', 0)
		if m > value_of_move:
			value_of_move = m
			best_move = move
		
	current_position = assign(current_position, best_move, 'O')
	print()
	printPosition(current_position)

	result = detect_result(current_position)
	if result==ongoing:
		current_position = inputMove(current_position, 'X')
		printPosition(current_position)
		result = detect_result(current_position)

	if result!=ongoing:
		print(result)
		stop = True